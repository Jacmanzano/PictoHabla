package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.JwtProperties("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 15, 7),
                new AppProperties.SecurityProperties(false, false, 5, 15),
                new AppProperties.CorsProperties(List.of("http://localhost:3000"))
        );
        jwtService = new JwtService(appProperties);
    }

    @Test
    void generateAndValidateAccessToken_Success() {
        UUID teacherId = UUID.randomUUID();
        String email = "profe@colegio.es";
        String fullName = "Profesor Carlos";

        String token = jwtService.generateAccessToken(teacherId, email, fullName);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(teacherId, jwtService.extractTeacherId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(900L, jwtService.getAccessTokenExpirationSeconds());
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtService.validateToken("token.invalido.falso"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void validateToken_AlteredSignature_ReturnsFalse() {
        UUID teacherId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(teacherId, "test@test.es", "Test");
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.validateToken(tamperedToken));
    }
}
