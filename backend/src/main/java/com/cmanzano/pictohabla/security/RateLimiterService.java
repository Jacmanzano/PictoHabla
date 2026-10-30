package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de limitación de tasa (Rate Limiting) y mitigación de ataques de fuerza bruta / DoS.
 * Permite proteger endpoints sensibles (login, registro, recuperación de contraseña, renovación de tokens)
 * basándose en la IP de origen, evitando que atacantes bloqueen maliciosamente las cuentas legítimas de profesores.
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

    /**
     * Extrae de forma segura la IP real del cliente.
     * Solo confía en cabeceras de proxy (CF-Connecting-IP, X-Forwarded-For) si la petición
     * procede de un origen de confianza (Loopback de TrueNAS o red interna Docker).
     * Si la petición proviene de un cliente LAN directo no confiable, se ignora cualquier
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

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        // 1. Loopback local (Cloudflare Tunnel ejecutándose en host network en TrueNAS)
        if (remoteAddr.equals("127.0.0.1") || remoteAddr.equals("::1") || remoteAddr.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        // 2. Red interna privada de Docker Bridge (172.16.0.0 - 172.31.255.255)
        if (remoteAddr.startsWith("172.")) {
            try {
                String[] parts = remoteAddr.split("\\.");
                if (parts.length >= 2) {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
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
        // Limpieza oportunista si la colección crece
        if (rateLimitBuckets.size() > 500) {
            rateLimitBuckets.entrySet().removeIf(entry -> now.isAfter(entry.getValue().resetTime));
        }
        if (failedLoginByIp.size() > 500) {
            failedLoginByIp.entrySet().removeIf(entry -> now.isAfter(entry.getValue().resetTime));
        }
    }
}
