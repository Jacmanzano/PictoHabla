package com.cmanzano.pictohabla.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        JwtProperties jwt,
        SecurityProperties security,
        CorsProperties cors
) {
    public record JwtProperties(
            String secret,
            long accessTokenExpirationMinutes,
            long refreshTokenExpirationDays
    ) {}

    public record SecurityProperties(
            boolean cookieSecure,
            boolean emailVerificationRequired,
            boolean teacherRegistrationEnabled,
            int maxFailedAttempts,
            long lockoutDurationMinutes
    ) {}

    public record CorsProperties(
            List<String> allowedOrigins
    ) {}
}
