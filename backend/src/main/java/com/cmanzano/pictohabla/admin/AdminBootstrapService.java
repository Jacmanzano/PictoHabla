package com.cmanzano.pictohabla.admin;

import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servicio transaccional y aislado para la creación del primer administrador.
 * 
 * Garantías de Concurrencia y Seguridad:
 * 1. Adquiere un bloqueo transaccional a nivel de motor PostgreSQL (pg_advisory_xact_lock)
 *    antes de evaluar el estado del sistema, impidiendo condiciones de carrera entre procesos independientes.
 * 2. Verifica la marca persistente en 'admin_bootstrap_metadata' para evitar reactivación incluso si
 *    la cuenta ADMIN original fuese eliminada manualmente.
 * 3. Valida la restricción a nivel relacional en 'teachers' (uq_teachers_single_admin).
 * 4. Protege contra ejecuciones en la misma JVM mediante monitor sincronizado estático.
 */
@Service
public class AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    // Identificador único de 64 bits para el bloqueo consultivo transaccional en PostgreSQL
    private static final long PG_BOOTSTRAP_ADVISORY_LOCK_ID = 7821948201948201L;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,100}$");

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;

    // Monitor en memoria para protección de concurrencia de hilos locales
    private static final Object JVM_LOCK = new Object();

    public AdminBootstrapService(
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate
    ) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ejecuta el bootstrap transaccional del primer administrador.
     * Garantiza exclusión mutua entre procesos independientes y entre hilos.
     */
    public Teacher bootstrapAdmin(String email, String rawPassword, String fullName) {
        synchronized (JVM_LOCK) {
            return transactionTemplate.execute(status -> {
                // 1. Adquirir bloqueo transaccional de PostgreSQL si estamos en dicho motor
                acquirePostgresAdvisoryLockIfApplicable();

                // 2. Comprobar si la marca persistente en admin_bootstrap_metadata ya existe
                if (isBootstrapMetadataPresent()) {
                    log.warn("Bootstrap denegado: admin_bootstrap_metadata indica que el bootstrap ya fue completado previamente.");
                    throw new IllegalStateException("El proceso de bootstrap ya fue completado previamente en este sistema y está desactivado de forma permanente.");
                }

                // 3. Comprobar si ya existe un usuario con rol ADMIN en la tabla teachers
                if (teacherRepository.countByRole(Role.ADMIN) > 0) {
                    log.warn("Bootstrap denegado: ya existe al menos un usuario con rol ADMIN en teachers.");
                    throw new IllegalStateException("Ya existe un administrador en el sistema. El proceso de bootstrap está desactivado permanentemente.");
                }

                // 4. Validar parámetros de entrada
                if (email == null || email.isBlank()) {
                    throw new IllegalArgumentException("El correo del administrador no puede estar vacío");
                }
                String normalizedEmail = email.trim().toLowerCase();
                if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                    throw new IllegalArgumentException("El formato del correo electrónico no es válido");
                }

                if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 150) {
                    throw new IllegalArgumentException("El nombre del administrador debe tener entre 2 y 150 caracteres");
                }

                if (rawPassword == null || !PASSWORD_PATTERN.matcher(rawPassword).matches()) {
                    throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");
                }

                if (teacherRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                    throw new IllegalArgumentException("El correo electrónico ya está registrado en el sistema");
                }

                // 5. Crear cuenta con rol inmutable ADMIN, estado ACTIVE y correo verificado
                Teacher admin = new Teacher(
                        UUID.randomUUID(),
                        normalizedEmail,
                        passwordEncoder.encode(rawPassword),
                        fullName.trim(),
                        TeacherStatus.ACTIVE,
                        Role.ADMIN
                );
                admin.setEmailVerified(true);

                Teacher saved;
                try {
                    saved = teacherRepository.saveAndFlush(admin);
                } catch (DataIntegrityViolationException ex) {
                    log.error("Violación de integridad al persistir el administrador (restricción uq_teachers_single_admin): {}", ex.getMessage());
                    throw new IllegalStateException("Violación de restricción relacional: ya existe un administrador en la base de datos.", ex);
                }

                // 6. Registrar la marca persistente de bootstrap completado en la base de datos
                try {
                    jdbcTemplate.update(
                            "INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email) VALUES (1, TRUE, CURRENT_TIMESTAMP, ?)",
                            saved.getEmail()
                    );
                } catch (DataIntegrityViolationException ex) {
                    log.error("Violación de clave única en admin_bootstrap_metadata (id=1 duplicado): {}", ex.getMessage());
                    throw new IllegalStateException("Violación de metadatos: el registro de bootstrap ya fue ejecutado por otra transacción concurrente.", ex);
                }

                log.info("Primer administrador creado con éxito en el sistema (ID: {}, Email: {}).", saved.getId(), saved.getEmail());
                return saved;
            });
        }
    }

    /**
     * Comprueba si el bootstrap ya fue realizado previamente, verificando tanto la tabla
     * persistente de metadatos como la tabla teachers.
     */
    public boolean isAdminAlreadyPresent() {
        if (isBootstrapMetadataPresent()) {
            return true;
        }
        return teacherRepository.countByRole(Role.ADMIN) > 0;
    }

    private boolean isBootstrapMetadataPresent() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_bootstrap_metadata", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void acquirePostgresAdvisoryLockIfApplicable() {
        if (isPostgreSqlDatabase()) {
            try {
                jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + PG_BOOTSTRAP_ADVISORY_LOCK_ID + ")");
                log.debug("Bloqueo transaccional pg_advisory_xact_lock({}) adquirido con éxito.", PG_BOOTSTRAP_ADVISORY_LOCK_ID);
            } catch (Exception ex) {
                log.warn("No se pudo adquirir pg_advisory_xact_lock: {}", ex.getMessage());
            }
        }
    }

    private boolean isPostgreSqlDatabase() {
        try {
            DataSource ds = jdbcTemplate.getDataSource();
            if (ds != null) {
                try (Connection conn = ds.getConnection()) {
                    String productName = conn.getMetaData().getDatabaseProductName();
                    return productName != null && productName.toLowerCase().contains("postgres");
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
