package com.cmanzano.pictohabla.classroom;

import com.cmanzano.pictohabla.classroom.dto.ClassroomRequest;
import com.cmanzano.pictohabla.classroom.dto.ClassroomResponse;
import com.cmanzano.pictohabla.classroom.dto.UpdateClassroomRequest;
import com.cmanzano.pictohabla.common.dto.ApiResponse;
import com.cmanzano.pictohabla.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms")
@PreAuthorize("hasRole('TEACHER')")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClassroomResponse>> createClassroom(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ClassroomRequest request
    ) {
        ClassroomResponse response = classroomService.createClassroom(securityUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Aula creada exitosamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> getClassrooms(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        List<ClassroomResponse> classrooms = classroomService.getClassroomsForTeacher(securityUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(classrooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassroomResponse>> getClassroomById(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("id") UUID id
    ) {
        ClassroomResponse response = classroomService.getClassroomById(securityUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassroomResponse>> updateClassroom(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateClassroomRequest request
    ) {
        ClassroomResponse response = classroomService.updateClassroom(securityUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Aula actualizada exitosamente", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClassroom(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable("id") UUID id
    ) {
        classroomService.deleteClassroom(securityUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Aula eliminada exitosamente"));
    }
}
