package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // Inicializa con la subred real de Cloudflare Tunnel en TrueNAS: 172.16.1.0/24
        rateLimiterService = new RateLimiterService("172.16.1.0/24");
    }

    @Test
    void getClientIp_TrustedLoopback_TrustsCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.195");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    void getClientIp_TrustedCloudflareSubnet_TrustsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // IP real del contenedor cloudflared en TrueNAS: 172.16.1.2 dentro de 172.16.1.0/24
        request.setRemoteAddr("172.16.1.2");
        request.addHeader("CF-Connecting-IP", "198.51.100.77");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("198.51.100.77", ip);
    }

    @Test
    void getClientIp_OtherDockerSubnet_IgnoresSpoofedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Otro contenedor en otro segmento Docker (ej. 172.16.2.10 fuera de 172.16.1.0/24)
        request.setRemoteAddr("172.16.2.10");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        // Debe ignorar la cabecera falseada y reportar la IP real de conexión
        assertEquals("172.16.2.10", ip);
    }

    @Test
    void getClientIp_UntrustedLanClient_IgnoresSpoofedCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.150");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("192.168.1.150", ip);
    }

    @Test
    void isInSubnet_CorrectMatching() {
        assertTrue(RateLimiterService.isInSubnet("172.16.1.2", "172.16.1.0/24"));
        assertTrue(RateLimiterService.isInSubnet("172.16.1.254", "172.16.1.0/24"));
        assertFalse(RateLimiterService.isInSubnet("172.16.2.1", "172.16.1.0/24"));
        assertFalse(RateLimiterService.isInSubnet("10.0.0.1", "172.16.1.0/24"));
    }

    @Test
    void checkRateLimit_ExceedingQuota_ThrowsTooManyRequests() {
        String clientIp = "192.168.1.50";
        for (int i = 0; i < 3; i++) {
            rateLimiterService.checkRateLimit("test_action", clientIp, 3, Duration.ofMinutes(1));
        }

        assertThrows(TooManyRequestsException.class, () ->
                rateLimiterService.checkRateLimit("test_action", clientIp, 3, Duration.ofMinutes(1))
        );
    }

    @Test
    void checkLoginAllowed_ExceedingFailedLogins_BlocksIp() {
        String attackerIp = "203.0.113.5";
        for (int i = 0; i < 10; i++) {
            rateLimiterService.recordFailedLogin(attackerIp);
        }

        assertThrows(TooManyRequestsException.class, () ->
                rateLimiterService.checkLoginAllowed(attackerIp)
        );

        rateLimiterService.resetFailedLogin(attackerIp);
        assertDoesNotThrow(() -> rateLimiterService.checkLoginAllowed(attackerIp));
    }
}
