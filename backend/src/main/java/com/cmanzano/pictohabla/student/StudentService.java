package com.cmanzano.pictohabla.student;

import com.cmanzano.pictohabla.classroom.Classroom;
import com.cmanzano.pictohabla.classroom.ClassroomRepository;
import com.cmanzano.pictohabla.common.exception.ConflictException;
import com.cmanzano.pictohabla.common.exception.ResourceNotFoundException;
import com.cmanzano.pictohabla.student.dto.CreateStudentRequest;
import com.cmanzano.pictohabla.student.dto.StudentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StudentService {

    private static final String PSEUDONYM_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;

    public StudentService(StudentRepository studentRepository, ClassroomRepository classroomRepository) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
    }

    public StudentResponse addStudentToClassroom(UUID teacherId, UUID classroomId, CreateStudentRequest request) {
        Classroom classroom = classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada o no pertenece al profesor"));

        String code = request.customCode() != null && !request.customCode().isBlank()
                ? request.customCode().trim().toUpperCase()
                : generateUniquePseudonym();

        if (studentRepository.existsByPseudonymCode(code)) {
            throw new ConflictException("El código pseudónimo ya está en uso");
        }

        Student student = new Student(
                UUID.randomUUID(),
                classroom,
                code,
                request.nickname()
        );

        Student saved = studentRepository.save(student);
        return StudentResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsInClassroom(UUID teacherId, UUID classroomId) {
        if (!classroomRepository.existsByIdAndTeacherId(classroomId, teacherId)) {
            throw new ResourceNotFoundException("Aula no encontrada o no pertenece al profesor");
        }

        return studentRepository.findByClassroomIdAndIsActiveTrue(classroomId)
                .stream()
                .map(StudentResponse::fromDomain)
                .toList();
    }

    private String generateUniquePseudonym() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder("ALU-");
            for (int j = 0; j < 6; j++) {
                int index = RANDOM.nextInt(PSEUDONYM_CHARACTERS.length());
                sb.append(PSEUDONYM_CHARACTERS.charAt(index));
            }
            String candidate = sb.toString();
            if (!studentRepository.existsByPseudonymCode(candidate)) {
                return candidate;
            }
        }
        return "ALU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
