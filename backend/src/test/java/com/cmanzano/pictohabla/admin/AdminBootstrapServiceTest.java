package com.cmanzano.pictohabla.admin;

import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminBootstrapServiceTest {

    @Autowired
    private AdminBootstrapService adminBootstrapService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Limpiar metadatos de bootstrap y administradores para cada test
        jdbcTemplate.execute("DELETE FROM admin_bootstrap_metadata");
        teacherRepository.findByRole(Role.ADMIN).forEach(teacherRepository::delete);
    }

    @Test
    void bootstrapAdmin_WhenNoAdminExists_CreatesSingleAdminSuccessfully() {
        assertEquals(0, teacherRepository.countByRole(Role.ADMIN));

        Teacher admin = adminBootstrapService.bootstrapAdmin(
                "admin.principal@colegio.es",
                "SuperPassword2026",
                "Administrador Principal"
        );

        assertNotNull(admin);
        assertNotNull(admin.getId());
        assertEquals("admin.principal@colegio.es", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(TeacherStatus.ACTIVE, admin.getStatus());
        assertTrue(admin.isEmailVerified());
        assertTrue(passwordEncoder.matches("SuperPassword2026", admin.getPasswordHash()));

        // Verificar que en base de datos existe exactamente 1 administrador
        assertEquals(1, teacherRepository.countByRole(Role.ADMIN));
        assertTrue(adminBootstrapService.isAdminAlreadyPresent());
    }

    @Test
    void bootstrapAdmin_WhenAdminAlreadyExists_FailsWithIllegalStateException() {
        // Primer bootstrap: exitoso
        adminBootstrapService.bootstrapAdmin(
                "admin1@colegio.es",
                "Password123",
                "Primer Admin"
        );
        assertEquals(1, teacherRepository.countByRole(Role.ADMIN));

        // Segundo bootstrap: debe fallar inmediatamente
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminBootstrapService.bootstrapAdmin(
                        "admin2@colegio.es",
                        "Password456",
                        "Segundo Admin Intento"
                )
        );

        assertTrue(ex.getMessage().contains("ya fue completado previamente") || ex.getMessage().contains("Ya existe un administrador"));
        // El conteo sigue siendo estrictamente 1
        assertEquals(1, teacherRepository.countByRole(Role.ADMIN));
    }

    @Test
    void bootstrapAdmin_ConcurrentExecutions_CreateExactlyOneAdmin() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        tasks.add(() -> {
            try {
                adminBootstrapService.bootstrapAdmin("concur1@colegio.es", "Password123", "Admin Concur 1");
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        tasks.add(() -> {
            try {
                adminBootstrapService.bootstrapAdmin("concur2@colegio.es", "Password123", "Admin Concur 2");
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        int successCount = 0;
        int failureCount = 0;
        for (Future<Boolean> result : results) {
            try {
                if (result.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (ExecutionException e) {
                failureCount++;
            }
        }

        // Exactamente uno debe tener éxito y uno debe fallar
        assertEquals(1, successCount, "Exactamente una ejecución concurrente debe tener éxito");
        assertEquals(1, failureCount, "Exactamente una ejecución concurrente debe fallar");
        assertEquals(1, teacherRepository.countByRole(Role.ADMIN), "Debe haber exactamente un administrador en la BD");
    }

    @Test
    void bootstrapAdmin_InvalidInputs_ThrowIllegalArgumentException() {
        // Email vacío o inválido
        assertThrows(IllegalArgumentException.class, () ->
                adminBootstrapService.bootstrapAdmin("", "Password123", "Admin")
        );
        assertThrows(IllegalArgumentException.class, () ->
                adminBootstrapService.bootstrapAdmin("no-es-email", "Password123", "Admin")
        );

        // Nombre muy corto
        assertThrows(IllegalArgumentException.class, () ->
                adminBootstrapService.bootstrapAdmin("admin@colegio.es", "Password123", "A")
        );

        // Contraseña débil (sin números o menor a 8 caracteres)
        assertThrows(IllegalArgumentException.class, () ->
                adminBootstrapService.bootstrapAdmin("admin@colegio.es", "cortita", "Admin Valido")
        );
        assertThrows(IllegalArgumentException.class, () ->
                adminBootstrapService.bootstrapAdmin("admin@colegio.es", "sololetraslargas", "Admin Valido")
        );
    }

    @Test
    void databaseConstraint_PreventsMultipleAdminRowsInTeachers() {
        Teacher admin1 = new Teacher(
                UUID.randomUUID(),
                "dbadmin1@colegio.es",
                passwordEncoder.encode("Pass1234"),
                "Admin DB 1",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        );
        teacherRepository.saveAndFlush(admin1);

        Teacher admin2 = new Teacher(
                UUID.randomUUID(),
                "dbadmin2@colegio.es",
                passwordEncoder.encode("Pass5678"),
                "Admin DB 2",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        );

        // La restricción uq_teachers_single_admin en base de datos debe impedir un segundo registro con role=ADMIN
        assertThrows(DataIntegrityViolationException.class, () -> {
            teacherRepository.saveAndFlush(admin2);
        });
    }

    @Test
    void databaseConstraint_AllowsMultipleTeacherRowsInTeachers() {
        Teacher teacher1 = new Teacher(
                UUID.randomUUID(),
                "dbprofe1@colegio.es",
                passwordEncoder.encode("Pass1234"),
                "Profe DB 1",
                TeacherStatus.ACTIVE,
                Role.TEACHER
        );
        Teacher teacher2 = new Teacher(
                UUID.randomUUID(),
                "dbprofe2@colegio.es",
                passwordEncoder.encode("Pass5678"),
                "Profe DB 2",
                TeacherStatus.ACTIVE,
                Role.TEACHER
        );

        // Múltiples profesores con role=TEACHER deben permitirse sin problema (admin_singleton es NULL)
        assertDoesNotThrow(() -> {
            teacherRepository.saveAndFlush(teacher1);
            teacherRepository.saveAndFlush(teacher2);
        });
    }

    @Test
    void databaseConstraint_AdminBootstrapMetadata_PreventsMultipleRecords() {
        // Primer registro: id=1 permitido
        jdbcTemplate.update(
                "INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email) VALUES (1, TRUE, ?, ?)",
                Instant.now(),
                "firstadmin@colegio.es"
        );

        // Intento de duplicar id=1: falla por Primary Key
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email) VALUES (1, TRUE, ?, ?)",
                    Instant.now(),
                    "secondadmin@colegio.es"
            );
        });

        // Intento de insertar id=2: falla por CHECK constraint (chk_single_bootstrap_record CHECK (id = 1))
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO admin_bootstrap_metadata (id, completed, completed_at, admin_email) VALUES (2, TRUE, ?, ?)",
                    Instant.now(),
                    "thirdadmin@colegio.es"
            );
        });
    }

    @Test
    void bootstrapAdmin_WhenMetadataExistsEvenIfAdminAccountDeleted_IsPermanentlyBlocked() {
        // 1. Ejecutar bootstrap inicial
        Teacher admin = adminBootstrapService.bootstrapAdmin(
                "original.admin@colegio.es",
                "Password123",
                "Admin Original"
        );
        assertNotNull(admin);

        // 2. Simular que la cuenta ADMIN fue eliminada manualmente de la base de datos
        teacherRepository.delete(admin);
        assertEquals(0, teacherRepository.countByRole(Role.ADMIN));

        // 3. Intento de re-ejecutar el bootstrap: DEBE ser rechazado por la marca persistente de admin_bootstrap_metadata
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            adminBootstrapService.bootstrapAdmin(
                    "nuevo.intento@colegio.es",
                    "Password999",
                    "Intento Segundo Bootstrap"
            );
        });

        assertTrue(ex.getMessage().contains("ya fue completado previamente"),
                "El mensaje debe indicar que el bootstrap ya fue completado previamente: " + ex.getMessage());
    }
}
