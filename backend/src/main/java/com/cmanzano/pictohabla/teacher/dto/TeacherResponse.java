package com.cmanzano.pictohabla.teacher.dto;

import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherStatus;

import java.time.Instant;
import java.util.UUID;

public record TeacherResponse(
        UUID id,
        String email,
        String fullName,
        TeacherStatus status,
        boolean emailVerified,
        Instant createdAt
) {
    public static TeacherResponse fromDomain(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getFullName(),
                teacher.getStatus(),
                teacher.isEmailVerified(),
                teacher.getCreatedAt()
        );
    }
}
