package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "pictohabla-backend";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;

    public JwtService(AppProperties appProperties) {
        String secret = appProperties.jwt() != null && appProperties.jwt().secret() != null
                ? appProperties.jwt().secret()
                : "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = appProperties.jwt() != null
                ? appProperties.jwt().accessTokenExpirationMinutes()
                : 15;
    }

    public String generateAccessToken(UUID teacherId, String email, String fullName) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(teacherId.toString())
                .claim("email", email)
                .claim("name", fullName)
                .claim("role", "ROLE_TEACHER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT no válido o expirado: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractTeacherId(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
