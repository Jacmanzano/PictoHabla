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
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REFRESH_COOKIE_NAME = "pictohabla_refresh";

    private final TeacherRepository teacherRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final com.cmanzano.pictohabla.email.EmailService emailService;
    private final com.cmanzano.pictohabla.security.RateLimiterService rateLimiterService;

    public AuthService(
            TeacherRepository teacherRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties
    ) {
        this(teacherRepository, refreshTokenRepository, passwordEncoder, jwtService, appProperties, null, new com.cmanzano.pictohabla.security.RateLimiterService());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(
            TeacherRepository teacherRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.cmanzano.pictohabla.email.EmailService emailService,
            com.cmanzano.pictohabla.security.RateLimiterService rateLimiterService
    ) {
        this.teacherRepository = teacherRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.emailService = emailService;
        this.rateLimiterService = rateLimiterService != null ? rateLimiterService : new com.cmanzano.pictohabla.security.RateLimiterService();
    }

    public TeacherResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (teacherRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("El correo electrónico ya está registrado");
        }

        boolean requireVerification = appProperties.security() != null && appProperties.security().emailVerificationRequired();
        TeacherStatus initialStatus = requireVerification ? TeacherStatus.PENDING_VERIFICATION : TeacherStatus.ACTIVE;

        Teacher teacher = new Teacher(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                initialStatus
        );

        if (requireVerification) {
            teacher.setEmailVerified(false);
            String token = UUID.randomUUID().toString();
            teacher.setVerificationToken(token);
            teacher.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
            if (emailService != null) {
                emailService.sendVerificationEmail(teacher.getEmail(), teacher.getFullName(), token);
            }
            log.info("Profesor registrado en modo producción. Pendiente de verificación por correo.");
        } else {
            teacher.setEmailVerified(true);
            log.info("Profesor registrado con éxito en modo desarrollo.");
        }

        Teacher saved = teacherRepository.save(teacher);
        return TeacherResponse.fromDomain(saved);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        return login(request, response, "0.0.0.0");
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response, String clientIp) {
        if (clientIp != null) {
            rateLimiterService.checkLoginAllowed(clientIp);
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        Teacher teacher = teacherRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> {
                    if (clientIp != null) {
                        rateLimiterService.recordFailedLogin(clientIp);
                    }
                    return new UnauthorizedException("Credenciales inválidas");
                });

        // Comprobación de bloqueo temporal por intentos fallidos
        if (!teacher.isAccountNonLocked()) {
            throw new AccountLockedException("La cuenta está temporalmente bloqueada por múltiples intentos fallidos. Inténtalo más tarde.");
        }

        // Verificación de contraseña segura
        if (!passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
            if (clientIp != null) {
                rateLimiterService.recordFailedLogin(clientIp);
            }
            handleFailedLogin(teacher);
            throw new UnauthorizedException("Credenciales inválidas");
        }

        if (clientIp != null) {
            rateLimiterService.resetFailedLogin(clientIp);
        }

        // Restablecer contador de fallos
        teacher.setFailedLoginAttempts(0);
        teacher.setLockedUntil(null);

        // Verificación de estado y correo
        boolean requireVerification = appProperties.security() != null && appProperties.security().emailVerificationRequired();
        if (requireVerification && !teacher.isEmailVerified()) {
            throw new UnauthorizedException("Debes verificar tu correo electrónico antes de iniciar sesión");
        }

        if (teacher.getStatus() == TeacherStatus.SUSPENDED) {
            throw new UnauthorizedException("La cuenta de profesor ha sido suspendida");
        }

        teacherRepository.save(teacher);

        return createAuthSession(teacher, response);
    }

    public AuthResponse refreshToken(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Token de actualización no proporcionado");
        }

        String hash = HashUtils.sha256Hex(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Token de actualización inválido o expirado"));

        Teacher teacher = storedToken.getTeacher();

        // Detección de reutilización (posible robo de token): revocar todas las sesiones del profesor
        if (storedToken.isRevoked()) {
            log.warn("¡ALERTA DE SEGURIDAD! Intento de reutilización de refresh token revocado para el profesor ID {}. Revocando todas las sesiones.", teacher.getId());
            refreshTokenRepository.revokeAllTeacherTokens(teacher.getId(), Instant.now());
            clearRefreshCookie(response);
            throw new UnauthorizedException("Sesión comprometida o revocada. Se han cerrado todas las sesiones por seguridad.");
        }

        if (storedToken.isExpired()) {
            throw new UnauthorizedException("El token de actualización ha expirado. Por favor, inicia sesión nuevamente.");
        }

        if (teacher.getStatus() == TeacherStatus.SUSPENDED) {
            throw new UnauthorizedException("La cuenta del profesor está suspendida");
        }

        // Rotación de token: generamos nuevo refresh token y marcamos el anterior como reemplazado
        String newRawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String newHash = HashUtils.sha256Hex(newRawRefreshToken);

        storedToken.revoke(newHash);
        refreshTokenRepository.save(storedToken);

        long refreshDays = appProperties.jwt() != null ? appProperties.jwt().refreshTokenExpirationDays() : 7;
        RefreshToken newToken = new RefreshToken(
                UUID.randomUUID(),
                teacher,
                newHash,
                Instant.now().plus(refreshDays, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(newToken);

        String newAccessToken = jwtService.generateAccessToken(teacher.getId(), teacher.getEmail(), teacher.getFullName());
        setRefreshCookie(response, newRawRefreshToken, refreshDays);

        return AuthResponse.of(
                newAccessToken,
                jwtService.getAccessTokenExpirationSeconds(),
                TeacherResponse.fromDomain(teacher),
                newRawRefreshToken
        );
    }

    public void logout(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = HashUtils.sha256Hex(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.revoke(null);
                refreshTokenRepository.save(token);
            });
        }
        clearRefreshCookie(response);
        SecurityContextHolder.clearContext();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        teacherRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(teacher -> {
            String token = UUID.randomUUID().toString();
            teacher.setPasswordResetToken(token);
            teacher.setPasswordResetExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            teacherRepository.save(teacher);
            if (emailService != null) {
                emailService.sendPasswordResetEmail(teacher.getEmail(), teacher.getFullName(), token);
            }
            log.info("Token de restablecimiento generado para profesor.");
        });
        // La respuesta al cliente no revela si el correo existe o no
    }

    public void resetPassword(ResetPasswordRequest request) {
        Teacher teacher = teacherRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new UnauthorizedException("El enlace de restablecimiento es inválido o ha expirado"));

        if (teacher.getPasswordResetExpiresAt() == null || Instant.now().isAfter(teacher.getPasswordResetExpiresAt())) {
            throw new UnauthorizedException("El enlace de restablecimiento ha expirado");
        }

        teacher.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        teacher.setPasswordResetToken(null);
        teacher.setPasswordResetExpiresAt(null);
        teacher.setFailedLoginAttempts(0);
        teacher.setLockedUntil(null);
        teacherRepository.save(teacher);

        // Por seguridad, revocamos todos los refresh tokens activos
        refreshTokenRepository.revokeAllTeacherTokens(teacher.getId(), Instant.now());
    }

    public void verifyEmail(String token) {
        Teacher teacher = teacherRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UnauthorizedException("El enlace de verificación es inválido"));

        if (teacher.getVerificationTokenExpiresAt() != null && Instant.now().isAfter(teacher.getVerificationTokenExpiresAt())) {
            throw new UnauthorizedException("El enlace de verificación ha expirado");
        }

        teacher.setEmailVerified(true);
        teacher.setStatus(TeacherStatus.ACTIVE);
        teacher.setVerificationToken(null);
        teacher.setVerificationTokenExpiresAt(null);
        teacherRepository.save(teacher);
    }

    @Transactional(readOnly = true)
    public TeacherResponse getCurrentTeacher(SecurityUser user) {
        if (user == null) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        Teacher teacher = teacherRepository.findById(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Profesor no encontrado"));
        return TeacherResponse.fromDomain(teacher);
    }

    private AuthResponse createAuthSession(Teacher teacher, HttpServletResponse response) {
        String accessToken = jwtService.generateAccessToken(teacher.getId(), teacher.getEmail(), teacher.getFullName());

        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = HashUtils.sha256Hex(rawRefreshToken);

        long refreshDays = appProperties.jwt() != null ? appProperties.jwt().refreshTokenExpirationDays() : 7;
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                teacher,
                tokenHash,
                Instant.now().plus(refreshDays, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        setRefreshCookie(response, rawRefreshToken, refreshDays);

        return AuthResponse.of(
                accessToken,
                jwtService.getAccessTokenExpirationSeconds(),
                TeacherResponse.fromDomain(teacher),
                rawRefreshToken
        );
    }

    private void handleFailedLogin(Teacher teacher) {
        int maxAttempts = appProperties.security() != null ? appProperties.security().maxFailedAttempts() : 5;
        long lockMinutes = appProperties.security() != null ? appProperties.security().lockoutDurationMinutes() : 15;

        int currentAttempts = teacher.getFailedLoginAttempts() + 1;
        teacher.setFailedLoginAttempts(currentAttempts);

        if (currentAttempts >= maxAttempts) {
            teacher.setLockedUntil(Instant.now().plus(lockMinutes, ChronoUnit.MINUTES));
            log.warn("Cuenta de profesor bloqueada temporalmente por exceso de intentos fallidos: {}", teacher.getEmail());
        }
        teacherRepository.save(teacher);
    }

    private void setRefreshCookie(HttpServletResponse response, String token, long days) {
        if (response == null) return;
        boolean secure = appProperties.security() != null && appProperties.security().cookieSecure();
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(days))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        if (response == null) return;
        boolean secure = appProperties.security() != null && appProperties.security().cookieSecure();
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
