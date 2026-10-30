package com.cmanzano.pictohabla.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassroomRequest(
        @NotBlank(message = "El nombre del aula es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description
) {}
