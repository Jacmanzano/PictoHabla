package com.cmanzano.pictohabla.student;

import com.cmanzano.pictohabla.common.dto.ApiResponse;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.student.dto.CreateStudentRequest;
import com.cmanzano.pictohabla.student.dto.StudentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/students")
@PreAuthorize("hasRole('TEACHER')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> addStudent(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("classroomId") UUID classroomId,
            @Valid @RequestBody CreateStudentRequest request
    ) {
        StudentResponse response = studentService.addStudentToClassroom(securityUser.getId(), classroomId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Alumno agregado exitosamente con código pseudónimo", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getStudents(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("classroomId") UUID classroomId
    ) {
        List<StudentResponse> students = studentService.getStudentsInClassroom(securityUser.getId(), classroomId);
        return ResponseEntity.ok(ApiResponse.ok(students));
    }
}
