package com.cmanzano.pictohabla.student.dto;

import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @Size(max = 50, message = "El apodo o identificador no puede superar 50 caracteres")
        String nickname,

        @Size(max = 50, message = "El código pseudónimo no puede superar 50 caracteres")
        String customCode
) {}
