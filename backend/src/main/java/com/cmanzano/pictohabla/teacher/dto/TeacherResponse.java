package com.cmanzano.pictohabla.teacher.dto;

import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherStatus;

import java.time.Instant;
import java.util.UUID;

public record TeacherResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        TeacherStatus status,
        boolean emailVerified,
        Instant createdAt
) {
    public static TeacherResponse fromDomain(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getEmail(),
                teacher.getFullName(),
                teacher.getRole() != null ? teacher.getRole() : Role.TEACHER,
                teacher.getStatus(),
                teacher.isEmailVerified(),
                teacher.getCreatedAt()
        );
    }

    public TeacherResponse(UUID id, String email, String fullName, TeacherStatus status, boolean emailVerified, Instant createdAt) {
        this(id, email, fullName, Role.TEACHER, status, emailVerified, createdAt);
    }
}
