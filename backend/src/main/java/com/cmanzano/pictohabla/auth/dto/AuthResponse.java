package com.cmanzano.pictohabla.auth.dto;

import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        TeacherResponse teacher,
        String refreshToken
) {
    public static AuthResponse of(String accessToken, long expiresInSeconds, TeacherResponse teacher, String refreshToken) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, teacher, refreshToken);
    }
}
