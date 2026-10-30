package com.cmanzano.pictohabla.teacher;

import com.cmanzano.pictohabla.common.exception.ResourceNotFoundException;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.cmanzano.pictohabla.teacher.dto.UpdateTeacherRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public TeacherResponse getTeacherProfile(UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con ID: " + teacherId));
        return TeacherResponse.fromDomain(teacher);
    }

    public TeacherResponse updateTeacherProfile(UUID teacherId, UpdateTeacherRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado con ID: " + teacherId));

        teacher.setFullName(request.fullName().trim());
        Teacher saved = teacherRepository.save(teacher);
        return TeacherResponse.fromDomain(saved);
    }
}
