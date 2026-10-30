package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de limitación de tasa (Rate Limiting) y mitigación de ataques de fuerza bruta / DoS.
 * Permite proteger endpoints sensibles basándose en la IP de origen, evitando que atacantes
 * bloqueen maliciosamente las cuentas legítimas de profesores.
 *
 * Incluye validación estricta de proxy de confianza: solo confía en CF-Connecting-IP si
 * la conexión TCP directa (request.getRemoteAddr()) coincide exactamente con TRUSTED_PROXY_IP
 * (IP fija del contenedor Cloudflare Tunnel, ej. 172.16.1.2).
 *
 * No se utiliza X-Forwarded-For como alternativa ni se confía en loopback por defecto en producción.
 * Si un cliente directo o un proxy no autorizado envía cabeceras, estas son completamente
 * ignoradas y se utiliza estrictamente la IP remota del socket TCP (request.getRemoteAddr()).
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final int MAX_FAILED_LOGINS_PER_IP = 10;
    private static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(15);

    private static class AttemptRecord {
        int count;
        Instant resetTime;

        AttemptRecord(int count, Instant resetTime) {
            this.count = count;
            this.resetTime = resetTime;
        }
    }

    private final Map<String, AttemptRecord> rateLimitBuckets = new ConcurrentHashMap<>();
    private final Map<String, AttemptRecord> failedLoginByIp = new ConcurrentHashMap<>();

    private volatile String trustedProxyIp;

    public RateLimiterService() {
        this("");
    }

    public RateLimiterService(@Value("${app.security.trusted-proxy-ip:}") String trustedProxyIp) {
        setTrustedProxyIp(trustedProxyIp);
    }

    public void setTrustedProxyIp(String trustedProxyIp) {
        this.trustedProxyIp = (trustedProxyIp != null && !trustedProxyIp.isBlank())
                ? trustedProxyIp.trim()
                : "";
    }

    public String getTrustedProxyIp() {
        return trustedProxyIp;
    }

    /**
     * Extrae de forma segura la IP real del cliente.
     * Solo confía en la cabecera CF-Connecting-IP si el peer TCP directo coincide
     * exactamente con TRUSTED_PROXY_IP.
     *
     * No se utiliza X-Forwarded-For como alternativa para evitar riesgos de inyección.
     * Si la petición no procede del proxy configurado o la cabecera es malformada/ausente,
     * se utiliza estrictamente request.getRemoteAddr().
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "0.0.0.0";

        if (isTrustedProxy(remoteAddr)) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                String candidate = cfIp.trim();
                if (isValidIp(candidate)) {
                    return candidate;
                }
                log.warn("Cabecera CF-Connecting-IP malformada recibida desde proxy de confianza {}: '{}'.", remoteAddr, cfIp);
            }
        }

        return remoteAddr;
    }

    /**
     * Comprueba si la dirección remota física del peer TCP coincide exactamente con el proxy de confianza.
     * No confía ciegamente en loopback ni en rangos generales.
     */
    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank() || trustedProxyIp == null || trustedProxyIp.isBlank()) {
            return false;
        }
        return remoteAddr.equals(trustedProxyIp);
    }

    /**
     * Valida sintácticamente una dirección IP (IPv4 o IPv6) sin realizar resolución DNS externa.
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String trimmed = ip.trim();
        return isValidIpv4(trimmed) || isValidIpv6(trimmed);
    }

    public static boolean isValidIpv4(String ip) {
        if (ip == null) return false;
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) return false;
            if (octet.length() > 1 && octet.startsWith("0")) return false;
            try {
                int val = Integer.parseInt(octet);
                if (val < 0 || val > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidIpv6(String ip) {
        if (ip == null || !ip.contains(":")) return false;
        String[] parts = ip.split(":", -1);
        if (parts.length < 3 || parts.length > 8) return false;
        for (String part : parts) {
            if (part.length() > 4) return false;
            for (char c : part.toCharArray()) {
                if (Character.digit(c, 16) == -1) return false;
            }
        }
        return true;
    }

    /**
     * Comprueba y aplica la cuota de peticiones por IP y acción.
     * Lanza TooManyRequestsException si se sobrepasa el límite.
     */
    public void checkRateLimit(String action, String clientIp, int maxAllowed, Duration window) {
        cleanExpiredEntries();

        String key = action + ":" + clientIp;
        Instant now = Instant.now();

        rateLimitBuckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.resetTime)) {
                return new AttemptRecord(1, now.plus(window));
            }
            if (existing.count >= maxAllowed) {
                log.warn("Límite de tasa excedido para acción '{}' desde IP: {}", action, clientIp);
                throw new TooManyRequestsException("Demasiadas solicitudes. Por favor, inténtalo de nuevo en unos minutos.");
            }
            existing.count++;
            return existing;
        });
    }

    /**
     * Comprueba si una IP tiene bloqueado el inicio de sesión por exceso de intentos fallidos.
     */
    public void checkLoginAllowed(String clientIp) {
        cleanExpiredEntries();
        AttemptRecord record = failedLoginByIp.get(clientIp);
        if (record != null && Instant.now().isBefore(record.resetTime) && record.count >= MAX_FAILED_LOGINS_PER_IP) {
            log.warn("IP bloqueada por fuerza bruta en inicio de sesión: {}", clientIp);
            throw new TooManyRequestsException("Demasiados intentos fallidos de inicio de sesión desde esta conexión. Espera 15 minutos.");
        }
    }

    /**
     * Registra un fallo de inicio de sesión asociado a la IP.
     */
    public void recordFailedLogin(String clientIp) {
        Instant now = Instant.now();
        failedLoginByIp.compute(clientIp, (ip, existing) -> {
            if (existing == null || now.isAfter(existing.resetTime)) {
                return new AttemptRecord(1, now.plus(FAILED_LOGIN_WINDOW));
            }
            existing.count++;
            return existing;
        });
    }

    /**
     * Restablece el contador de fallos de la IP tras un inicio de sesión exitoso.
     */
    public void resetFailedLogin(String clientIp) {
        failedLoginByIp.remove(clientIp);
    }

    private void cleanExpiredEntries() {
        Instant now = Instant.now();
        if (rateLimitBuckets.size() > 500) {
            rateLimitBuckets.entrySet().removeIf(entry -> now.isAfter(entry.getValue().resetTime));
        }
        if (failedLoginByIp.size() > 500) {
            failedLoginByIp.entrySet().removeIf(entry -> now.isAfter(entry.getValue().resetTime));
        }
    }
}
