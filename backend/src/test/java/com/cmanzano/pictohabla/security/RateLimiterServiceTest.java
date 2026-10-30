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
        rateLimiterService = new RateLimiterService();
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
    void getClientIp_TrustedDockerBridge_TrustsXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.42, 172.18.0.1");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("198.51.100.42", ip);
    }

    @Test
    void getClientIp_UntrustedLanClient_IgnoresSpoofedCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.150");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        // Debe ignorar la cabecera inyectada y retornar la IP real directa
        assertEquals("192.168.1.150", ip);
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

        // Tras resetear (login exitoso o desbloqueo), debe permitirse
        rateLimiterService.resetFailedLogin(attackerIp);
        assertDoesNotThrow(() -> rateLimiterService.checkLoginAllowed(attackerIp));
    }
}
