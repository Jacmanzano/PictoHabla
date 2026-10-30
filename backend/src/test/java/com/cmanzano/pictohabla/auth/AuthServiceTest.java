package com.cmanzano.pictohabla.auth;

import com.cmanzano.pictohabla.auth.dto.AuthResponse;
import com.cmanzano.pictohabla.auth.dto.ForgotPasswordRequest;
import com.cmanzano.pictohabla.auth.dto.LoginRequest;
import com.cmanzano.pictohabla.auth.dto.RegisterRequest;
import com.cmanzano.pictohabla.auth.dto.ResetPasswordRequest;
import com.cmanzano.pictohabla.auth.model.RefreshToken;
import com.cmanzano.pictohabla.auth.model.RefreshTokenRepository;
import com.cmanzano.pictohabla.common.exception.AccountLockedException;
import com.cmanzano.pictohabla.common.exception.ConflictException;
import com.cmanzano.pictohabla.common.exception.UnauthorizedException;
import com.cmanzano.pictohabla.common.util.HashUtils;
import com.cmanzano.pictohabla.config.AppProperties;
import com.cmanzano.pictohabla.security.JwtService;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AppProperties appProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        appProperties = new AppProperties(
                new AppProperties.JwtProperties("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 15, 7),
                new AppProperties.SecurityProperties(false, false, 5, 15),
                new AppProperties.CorsProperties(List.of("http://localhost:3000"))
        );
        jwtService = new JwtService(appProperties);
        authService = new AuthService(teacherRepository, refreshTokenRepository, passwordEncoder, jwtService, appProperties);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("Profe@Colegio.es ", "Password123", "Profesor Carlos");
        when(teacherRepository.existsByEmailIgnoreCase("profe@colegio.es")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("profe@colegio.es", response.email());
        assertEquals("Profesor Carlos", response.fullName());
        assertTrue(response.emailVerified());
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsConflictException() {
        RegisterRequest request = new RegisterRequest("repetido@colegio.es", "Password123", "Profesor");
        when(teacherRepository.existsByEmailIgnoreCase("repetido@colegio.es")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void login_Success() {
        String rawPassword = "Password123";
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", passwordEncoder.encode(rawPassword), "Carlos", TeacherStatus.ACTIVE);
        teacher.setEmailVerified(true);

        when(teacherRepository.findByEmailIgnoreCase("profe@colegio.es")).thenReturn(Optional.of(teacher));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse authResponse = authService.login(new LoginRequest("profe@colegio.es", rawPassword), response);

        assertNotNull(authResponse);
        assertNotNull(authResponse.accessToken());
        assertNotNull(authResponse.refreshToken());
        assertEquals("profe@colegio.es", authResponse.teacher().email());
        assertNotNull(response.getHeader("Set-Cookie"));
    }

    @Test
    void login_WrongPassword_IncrementsFailedAttemptsAndThrowsUnauthorized() {
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", passwordEncoder.encode("RealPassword1"), "Carlos", TeacherStatus.ACTIVE);
        when(teacherRepository.findByEmailIgnoreCase("profe@colegio.es")).thenReturn(Optional.of(teacher));

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("profe@colegio.es", "WrongPass123"), new MockHttpServletResponse())
        );

        assertEquals(1, teacher.getFailedLoginAttempts());
        verify(teacherRepository).save(teacher);
    }

    @Test
    void login_ExceedsMaxAttempts_LocksAccount() {
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", passwordEncoder.encode("RealPassword1"), "Carlos", TeacherStatus.ACTIVE);
        teacher.setFailedLoginAttempts(4); // Al siguiente intento llega a 5

        when(teacherRepository.findByEmailIgnoreCase("profe@colegio.es")).thenReturn(Optional.of(teacher));

        assertThrows(UnauthorizedException.class, () ->
                authService.login(new LoginRequest("profe@colegio.es", "WrongPass123"), new MockHttpServletResponse())
        );

        assertEquals(5, teacher.getFailedLoginAttempts());
        assertNotNull(teacher.getLockedUntil());
        assertTrue(teacher.getLockedUntil().isAfter(Instant.now()));
    }

    @Test
    void login_AccountLocked_ThrowsAccountLockedException() {
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", passwordEncoder.encode("RealPassword1"), "Carlos", TeacherStatus.ACTIVE);
        teacher.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(teacherRepository.findByEmailIgnoreCase("profe@colegio.es")).thenReturn(Optional.of(teacher));

        assertThrows(AccountLockedException.class, () ->
                authService.login(new LoginRequest("profe@colegio.es", "RealPassword1"), new MockHttpServletResponse())
        );
    }

    @Test
    void refreshToken_Success_RotatesToken() {
        String rawOldToken = "token-viejo-12345";
        String oldHash = HashUtils.sha256Hex(rawOldToken);

        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", "hash", "Carlos", TeacherStatus.ACTIVE);
        RefreshToken storedToken = new RefreshToken(UUID.randomUUID(), teacher, oldHash, Instant.now().plus(5, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse result = authService.refreshToken(rawOldToken, response);

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());
        assertNotEquals(rawOldToken, result.refreshToken());
        assertTrue(storedToken.isRevoked());
        assertNotNull(storedToken.getReplacedByTokenHash());
    }

    @Test
    void refreshToken_ReuseOfRevokedToken_RevokesAllSessions() {
        String stolenToken = "token-robado-revocado";
        String stolenHash = HashUtils.sha256Hex(stolenToken);

        UUID teacherId = UUID.randomUUID();
        Teacher teacher = new Teacher(teacherId, "profe@colegio.es", "hash", "Carlos", TeacherStatus.ACTIVE);

        RefreshToken alreadyRevoked = new RefreshToken(UUID.randomUUID(), teacher, stolenHash, Instant.now().plus(2, ChronoUnit.DAYS));
        alreadyRevoked.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(stolenHash)).thenReturn(Optional.of(alreadyRevoked));

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(stolenToken, response));

        // Debe haber revocado todas las sesiones de este profesor por seguridad
        verify(refreshTokenRepository).revokeAllTeacherTokens(eq(teacherId), any(Instant.class));
    }

    @Test
    void resetPassword_Success() {
        String resetToken = UUID.randomUUID().toString();
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", "oldHash", "Carlos", TeacherStatus.ACTIVE);
        teacher.setPasswordResetToken(resetToken);
        teacher.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

        when(teacherRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(teacher));

        authService.resetPassword(new ResetPasswordRequest(resetToken, "NewSecret123"));

        assertTrue(passwordEncoder.matches("NewSecret123", teacher.getPasswordHash()));
        assertNull(teacher.getPasswordResetToken());
        verify(refreshTokenRepository).revokeAllTeacherTokens(eq(teacher.getId()), any(Instant.class));
    }
}
