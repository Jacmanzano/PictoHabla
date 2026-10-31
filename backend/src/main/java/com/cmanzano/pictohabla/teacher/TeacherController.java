package com.cmanzano.pictohabla.teacher;

import com.cmanzano.pictohabla.common.dto.ApiResponse;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.cmanzano.pictohabla.teacher.dto.UpdateTeacherRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TeacherResponse>> getProfile(@AuthenticationPrincipal SecurityUser securityUser) {
        TeacherResponse response = teacherService.getTeacherProfile(securityUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateProfile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody UpdateTeacherRequest request
    ) {
        TeacherResponse response = teacherService.updateTeacherProfile(securityUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Perfil de profesor actualizado con éxito", response));
    }
}
