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
 * Incluye validación estricta de proxies de confianza: solo confía en CF-Connecting-IP o
 * X-Forwarded-For si la conexión directa proviene de la subred autorizada (ej. 172.16.1.0/24 de Cloudflare Tunnel).
 * Esto impide que otros contenedores en otros segmentos Docker o clientes LAN falseen direcciones IP.
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

    private final String trustedProxySubnet;

    public RateLimiterService() {
        this("172.16.1.0/24");
    }

    public RateLimiterService(@Value("${app.security.trusted-proxy-subnet:172.16.1.0/24}") String trustedProxySubnet) {
        this.trustedProxySubnet = (trustedProxySubnet != null && !trustedProxySubnet.isBlank())
                ? trustedProxySubnet.trim()
                : "172.16.1.0/24";
    }

    /**
     * Extrae de forma segura la IP real del cliente.
     * Solo confía en cabeceras de proxy (CF-Connecting-IP, X-Forwarded-For) si la petición
     * procede estrictamente de un origen de confianza (Loopback o la subred real de Cloudflare Tunnel).
     * Si la petición proviene de un origen no confiable, se ignora cualquier
     * cabecera inyectada y se utiliza estrictamente request.getRemoteAddr().
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "0.0.0.0";

        if (isTrustedProxy(remoteAddr)) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                return cfIp.trim();
            }
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] parts = xff.split(",");
                return parts[0].trim();
            }
        }

        return remoteAddr;
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        // 1. Loopback local (pruebas internas, healthchecks, etc.)
        if (remoteAddr.equals("127.0.0.1") || remoteAddr.equals("::1") || remoteAddr.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        // 2. Subred configurada de Cloudflare Tunnel (ej. 172.16.1.0/24)
        return isInSubnet(remoteAddr, trustedProxySubnet);
    }

    /**
     * Comprueba si una dirección IPv4 pertenece a una subred en notación CIDR.
     */
    public static boolean isInSubnet(String ipStr, String cidr) {
        if (cidr == null || cidr.isBlank() || ipStr == null || ipStr.isBlank()) {
            return false;
        }
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return ipStr.equals(cidr);
        }
        try {
            long ip = ipToLong(ipStr);
            long subnet = ipToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) return false;
            long mask = prefix == 0 ? 0 : (~0L << (32 - prefix)) & 0xFFFFFFFFL;
            return (ip & mask) == (subnet & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipToLong(String ipStr) {
        String[] octets = ipStr.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Dirección IPv4 no válida: " + ipStr);
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            long octet = Long.parseLong(octets[i].trim());
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Octeto IPv4 fuera de rango: " + octet);
            }
            result = (result << 8) | octet;
        }
        return result;
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
