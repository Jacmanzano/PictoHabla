package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // Inicializa con la IP fija exacta de Cloudflare Tunnel en TrueNAS: 172.16.1.2
        rateLimiterService = new RateLimiterService("172.16.1.2");
    }

    @Test
    @DisplayName("Petición desde la IP de Cloudflare (172.16.1.2) con CF-Connecting-IP -> confía en la cabecera")
    void getClientIp_TrustedCloudflareIp_TrustsHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");
        request.addHeader("CF-Connecting-IP", "198.51.100.77");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("198.51.100.77", ip);
    }

    @Test
    @DisplayName("En producción NO se confía en loopback (127.0.0.1) salvo que sea la IP de proxy configurada")
    void getClientIp_Loopback_IsNotTrustedByDefaultInProd() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.195");

        String ip = rateLimiterService.getClientIp(request);
        // Debe ignorar la cabecera y retornar 127.0.0.1 porque 127.0.0.1 != 172.16.1.2
        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("Loopback es de confianza solo si se configura explícitamente como proxy")
    void getClientIp_Loopback_IsTrustedIfConfiguredExplicitly() {
        rateLimiterService.setTrustedProxyIp("127.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.195");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("Otro contenedor en la misma subred (ej. 172.16.1.3) NO es de confianza -> ignora cabecera")
    void getClientIp_OtherPeerSameSubnet_IgnoresSpoofedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.3");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.16.1.3", ip);
    }

    @Test
    @DisplayName("Contenedor en otra red Docker (172.16.2.10) -> ignora cabecera")
    void getClientIp_OtherDockerSubnet_IgnoresSpoofedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.2.10");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.16.2.10", ip);
    }

    @Test
    @DisplayName("Cliente LAN directo (192.168.1.150) -> ignora cabecera CF-Connecting-IP falseada")
    void getClientIp_UntrustedLanClient_IgnoresSpoofedCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.150");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("192.168.1.150", ip);
    }

    @Test
    @DisplayName("Petición directa con X-Forwarded-For falsificada -> ignora cabecera y retorna peer TCP")
    void getClientIp_DirectConnection_WithSpoofedXForwardedFor_ReturnsDirectTcpIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.88");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("192.168.1.88", ip);
    }

    @Test
    @DisplayName("NO utilizar X-Forwarded-For como alternativa (ni siquiera desde Cloudflare)")
    void getClientIp_FromCloudflared_DoesNotUseXForwardedForAsFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");
        // No envía CF-Connecting-IP, solo envía X-Forwarded-For
        request.addHeader("X-Forwarded-For", "198.51.100.44");

        String ip = rateLimiterService.getClientIp(request);
        // Debe descartar X-Forwarded-For y devolver la IP física del proxy
        assertEquals("172.16.1.2", ip);
    }

    @Test
    @DisplayName("Petición desde Cloudflared con cabeceras ausentes -> fallback seguro a remoteAddr")
    void getClientIp_FromCloudflared_WithMissingHeaders_FallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.16.1.2", ip);
    }

    @Test
    @DisplayName("Petición desde Cloudflared con CF-Connecting-IP malformada -> fallback a remoteAddr")
    void getClientIp_FromCloudflared_WithMalformedCfIp_FallsBackSafely() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");
        request.addHeader("CF-Connecting-IP", "not_an_ip; DROP TABLE users;");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.16.1.2", ip);
    }

    @Test
    @DisplayName("Petición con cambio dinámico de IP de proxy -> actualiza confianza de inmediato")
    void getClientIp_IpChange_DynamicallyUpdatesTrust() {
        MockHttpServletRequest reqOld = new MockHttpServletRequest();
        reqOld.setRemoteAddr("172.16.1.2");
        reqOld.addHeader("CF-Connecting-IP", "203.0.113.50");

        MockHttpServletRequest reqNew = new MockHttpServletRequest();
        reqNew.setRemoteAddr("10.0.0.2");
        reqNew.addHeader("CF-Connecting-IP", "203.0.113.50");

        assertEquals("203.0.113.50", rateLimiterService.getClientIp(reqOld));
        assertEquals("10.0.0.2", rateLimiterService.getClientIp(reqNew));

        // Reconfigurar IP de proxy en caliente
        rateLimiterService.setTrustedProxyIp("10.0.0.2");
        assertEquals("10.0.0.2", rateLimiterService.getTrustedProxyIp());

        assertEquals("172.16.1.2", rateLimiterService.getClientIp(reqOld));
        assertEquals("203.0.113.50", rateLimiterService.getClientIp(reqNew));
    }

    @Test
    @DisplayName("Validación sintáctica de IPs IPv4 e IPv6 sin DNS")
    void isValidIp_UnitTests() {
        assertTrue(RateLimiterService.isValidIp("127.0.0.1"));
        assertTrue(RateLimiterService.isValidIp("172.16.1.2"));
        assertTrue(RateLimiterService.isValidIp("8.8.8.8"));
        assertTrue(RateLimiterService.isValidIp("::1"));
        assertTrue(RateLimiterService.isValidIp("2001:db8::1"));

        assertFalse(RateLimiterService.isValidIp(null));
        assertFalse(RateLimiterService.isValidIp(""));
        assertFalse(RateLimiterService.isValidIp("256.0.0.1"));
        assertFalse(RateLimiterService.isValidIp("evil.com"));
        assertFalse(RateLimiterService.isValidIp("192.168.1.1; ls"));
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
