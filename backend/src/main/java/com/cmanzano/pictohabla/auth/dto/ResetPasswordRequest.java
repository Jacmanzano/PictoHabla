package com.cmanzano.pictohabla.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El token de restablecimiento es obligatorio")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra y un número")
        String newPassword
) {}
