package com.cmanzano.pictohabla.classroom;

import com.cmanzano.pictohabla.classroom.dto.ClassroomRequest;
import com.cmanzano.pictohabla.classroom.dto.ClassroomResponse;
import com.cmanzano.pictohabla.classroom.dto.UpdateClassroomRequest;
import com.cmanzano.pictohabla.common.exception.ForbiddenException;
import com.cmanzano.pictohabla.common.exception.ResourceNotFoundException;
import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;

    public ClassroomService(ClassroomRepository classroomRepository, TeacherRepository teacherRepository) {
        this.classroomRepository = classroomRepository;
        this.teacherRepository = teacherRepository;
    }

    public ClassroomResponse createClassroom(UUID teacherId, ClassroomRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Solo los usuarios con rol de profesor pueden gestionar aulas de comunicación.");
        }

        Classroom classroom = new Classroom(
                UUID.randomUUID(),
                teacher,
                request.name(),
                request.description()
        );

        Classroom saved = classroomRepository.save(classroom);
        return ClassroomResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> getClassroomsForTeacher(UUID teacherId) {
        return classroomRepository.findByTeacherIdAndIsActiveTrueOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(ClassroomResponse::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassroomResponse getClassroomById(UUID teacherId, UUID classroomId) {
        Classroom classroom = classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada"));
        return ClassroomResponse.fromDomain(classroom);
    }

    public ClassroomResponse updateClassroom(UUID teacherId, UUID classroomId, UpdateClassroomRequest request) {
        Classroom classroom = classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada"));

        if (request.name() != null && !request.name().isBlank()) {
            classroom.setName(request.name().trim());
        }
        if (request.description() != null) {
            classroom.setDescription(request.description().trim());
        }
        if (request.isActive() != null) {
            classroom.setActive(request.isActive());
        }

        Classroom saved = classroomRepository.save(classroom);
        return ClassroomResponse.fromDomain(saved);
    }

    public void deleteClassroom(UUID teacherId, UUID classroomId) {
        Classroom classroom = classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula no encontrada"));

        // Desactivación lógica (soft delete) para proteger integridad referencial de los alumnos
        classroom.setActive(false);
        classroomRepository.save(classroom);
    }
}
