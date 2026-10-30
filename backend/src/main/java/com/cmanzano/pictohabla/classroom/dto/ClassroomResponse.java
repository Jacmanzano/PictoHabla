package com.cmanzano.pictohabla.classroom.dto;

import com.cmanzano.pictohabla.classroom.Classroom;

import java.time.Instant;
import java.util.UUID;

public record ClassroomResponse(
        UUID id,
        UUID teacherId,
        String name,
        String description,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClassroomResponse fromDomain(Classroom classroom) {
        return new ClassroomResponse(
                classroom.getId(),
                classroom.getTeacher().getId(),
                classroom.getName(),
                classroom.getDescription(),
                classroom.isActive(),
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }
}
