package com.cmanzano.pictohabla.student.dto;

import com.cmanzano.pictohabla.student.Student;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        UUID classroomId,
        String pseudonymCode,
        String nickname,
        boolean isActive,
        Instant createdAt
) {
    public static StudentResponse fromDomain(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getClassroom().getId(),
                student.getPseudonymCode(),
                student.getNickname(),
                student.isActive(),
                student.getCreatedAt()
        );
    }
}
