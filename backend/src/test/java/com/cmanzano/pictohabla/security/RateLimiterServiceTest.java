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
        // Inicializa con la subred real de Cloudflare Tunnel en TrueNAS: 172.16.1.0/24
        rateLimiterService = new RateLimiterService("172.16.1.0/24");
    }

    // =========================================================================
    // Pruebas existentes preservadas
    // =========================================================================

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

    // =========================================================================
    // Nuevas pruebas específicas solicitadas en la auditoría de seguridad
    // =========================================================================

    @Test
    @DisplayName("Petición directa con X-Forwarded-For falsificada -> debe ignorar la cabecera y retornar el peer TCP")
    void getClientIp_DirectConnection_WithSpoofedXForwardedFor_ReturnsDirectTcpIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.88");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("192.168.1.88", ip, "No debe confiar en X-Forwarded-For de clientes directos fuera de la subred");
    }

    @Test
    @DisplayName("Petición directa con CF-Connecting-IP y XFF falsificadas a la vez -> retorna peer TCP")
    void getClientIp_DirectConnection_WithBothSpoofedHeaders_ReturnsDirectTcpIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.88");
        request.addHeader("CF-Connecting-IP", "1.1.1.1");
        request.addHeader("X-Forwarded-For", "2.2.2.2");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("192.168.1.88", ip);
    }

    @Test
    @DisplayName("Petición desde proxy en subred no confiable -> retorna IP física del proxy no confiable")
    void getClientIp_ProxyFromUntrustedSubnet_ReturnsProxyTcpIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // Conexión procedente de otra subred Docker distinta (ej. 172.18.0.5)
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "100.64.0.1");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.18.0.5", ip);
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
    @DisplayName("Petición desde Cloudflared con CF-Connecting-IP malformada -> fallback a XFF o remoteAddr")
    void getClientIp_FromCloudflared_WithMalformedCfIp_FallsBackSafely() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");
        // Inyección malformada en CF-Connecting-IP
        request.addHeader("CF-Connecting-IP", "not_an_ip; DROP TABLE users;");
        // X-Forwarded-For válida
        request.addHeader("X-Forwarded-For", "198.51.100.44");

        String ip = rateLimiterService.getClientIp(request);
        // Debe ignorar la cabecera malformada y evaluar la siguiente válida
        assertEquals("198.51.100.44", ip);
    }

    @Test
    @DisplayName("Petición desde Cloudflared con ambas cabeceras malformadas -> fallback a remoteAddr")
    void getClientIp_FromCloudflared_WithBothHeadersMalformed_FallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.1.2");
        request.addHeader("CF-Connecting-IP", "999.999.999.999");
        request.addHeader("X-Forwarded-For", "invalid_xff_ip");

        String ip = rateLimiterService.getClientIp(request);
        assertEquals("172.16.1.2", ip);
    }

    @Test
    @DisplayName("Petición con cambio dinámico de subred -> actualiza confianza de forma inmediata")
    void getClientIp_SubnetChange_DynamicallyUpdatesTrust() {
        MockHttpServletRequest reqOldSubnet = new MockHttpServletRequest();
        reqOldSubnet.setRemoteAddr("172.16.1.2");
        reqOldSubnet.addHeader("CF-Connecting-IP", "203.0.113.50");

        MockHttpServletRequest reqNewSubnet = new MockHttpServletRequest();
        reqNewSubnet.setRemoteAddr("10.0.0.2");
        reqNewSubnet.addHeader("CF-Connecting-IP", "203.0.113.50");

        // Estado inicial: subred 172.16.1.0/24
        assertEquals("203.0.113.50", rateLimiterService.getClientIp(reqOldSubnet), "172.16.1.2 debe ser de confianza");
        assertEquals("10.0.0.2", rateLimiterService.getClientIp(reqNewSubnet), "10.0.0.2 NO debe ser de confianza aún");

        // Cambio de subred en caliente (ej. reconfiguración o entorno distinto)
        rateLimiterService.setTrustedProxySubnet("10.0.0.0/24");
        assertEquals("10.0.0.0/24", rateLimiterService.getTrustedProxySubnet());

        // Ahora se invierte la confianza:
        assertEquals("172.16.1.2", rateLimiterService.getClientIp(reqOldSubnet), "172.16.1.2 ya NO debe ser de confianza");
        assertEquals("203.0.113.50", rateLimiterService.getClientIp(reqNewSubnet), "10.0.0.2 ahora DEBE ser de confianza");
    }

    @Test
    @DisplayName("Validación sintáctica estricta de IPs (IPv4 e IPv6)")
    void isValidIp_UnitTests() {
        // IPv4 válidas
        assertTrue(RateLimiterService.isValidIp("127.0.0.1"));
        assertTrue(RateLimiterService.isValidIp("172.16.1.2"));
        assertTrue(RateLimiterService.isValidIp("8.8.8.8"));
        assertTrue(RateLimiterService.isValidIp("255.255.255.255"));

        // IPv6 válidas
        assertTrue(RateLimiterService.isValidIp("::1"));
        assertTrue(RateLimiterService.isValidIp("2001:db8::1"));
        assertTrue(RateLimiterService.isValidIp("fe80::1ff:fe23:4567:890a"));

        // Inválidas / Malformadas
        assertFalse(RateLimiterService.isValidIp(null));
        assertFalse(RateLimiterService.isValidIp(""));
        assertFalse(RateLimiterService.isValidIp("   "));
        assertFalse(RateLimiterService.isValidIp("256.0.0.1"));
        assertFalse(RateLimiterService.isValidIp("1.2.3.4.5"));
        assertFalse(RateLimiterService.isValidIp("1.2.3"));
        assertFalse(RateLimiterService.isValidIp("01.02.03.04")); // leading zero
        assertFalse(RateLimiterService.isValidIp("evil.com"));
        assertFalse(RateLimiterService.isValidIp("192.168.1.1; rm -rf"));
        assertFalse(RateLimiterService.isValidIp("192.168.1.1, 10.0.0.1"));
    }
}
