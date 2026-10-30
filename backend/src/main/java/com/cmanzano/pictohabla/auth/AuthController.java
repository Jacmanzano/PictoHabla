package com.cmanzano.pictohabla.auth;

import com.cmanzano.pictohabla.auth.dto.AuthResponse;
import com.cmanzano.pictohabla.auth.dto.ForgotPasswordRequest;
import com.cmanzano.pictohabla.auth.dto.LoginRequest;
import com.cmanzano.pictohabla.auth.dto.RefreshTokenRequest;
import com.cmanzano.pictohabla.auth.dto.RegisterRequest;
import com.cmanzano.pictohabla.auth.dto.ResetPasswordRequest;
import com.cmanzano.pictohabla.common.dto.ApiResponse;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TeacherResponse>> register(@Valid @RequestBody RegisterRequest request) {
        TeacherResponse teacher = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Profesor registrado exitosamente", teacher));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok("Inicio de sesión exitoso", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "pictohabla_refresh", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            HttpServletResponse response
    ) {
        String tokenToUse = bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()
                ? bodyRequest.refreshToken()
                : cookieRefreshToken;

        AuthResponse authResponse = authService.refreshToken(tokenToUse, response);
        return ResponseEntity.ok(ApiResponse.ok("Sesión renovada con éxito", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "pictohabla_refresh", required = false) String cookieRefreshToken,
            @RequestBody(required = false) RefreshTokenRequest bodyRequest,
            HttpServletResponse response
    ) {
        String tokenToUse = bodyRequest != null && bodyRequest.refreshToken() != null && !bodyRequest.refreshToken().isBlank()
                ? bodyRequest.refreshToken()
                : cookieRefreshToken;

        authService.logout(tokenToUse, response);
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada con éxito"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Si el correo está registrado, se han enviado las instrucciones de restablecimiento"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada exitosamente"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Correo electrónico verificado exitosamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TeacherResponse>> getCurrentUser(@AuthenticationPrincipal SecurityUser securityUser) {
        TeacherResponse teacher = authService.getCurrentTeacher(securityUser);
        return ResponseEntity.ok(ApiResponse.ok(teacher));
    }
}
