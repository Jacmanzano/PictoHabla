package com.cmanzano.pictohabla.migration;

import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationV2Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @Transactional
    void migration_V1_to_V2_PreservesExistingUsers_AndAppliesDefaultTeacherRole() {
        UUID testId = UUID.randomUUID();
        Instant now = Instant.now();

        // 1. Insertar directamente vía SQL simulando un registro existente antes de V2
        // (utilizando el valor por defecto de la columna role añadida por V2)
        jdbcTemplate.update(
                "INSERT INTO teachers (id, email, password_hash, full_name, status, email_verified, failed_login_attempts, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                testId,
                "migrado@colegio.es",
                "$2a$12$eX4mpleHashPlaceholderForMigrationTestOnly1234567890",
                "Profesor Preexistente",
                "ACTIVE",
                true,
                0,
                now,
                now
        );

        // 2. Comprobar a través de JPA que el registro se lee con Role.TEACHER automático
        Teacher migratedTeacher = teacherRepository.findById(testId).orElseThrow();
        assertEquals("migrado@colegio.es", migratedTeacher.getEmail());
        assertEquals("Profesor Preexistente", migratedTeacher.getFullName());
        assertEquals(Role.TEACHER, migratedTeacher.getRole(), "Todo profesor preexistente debe convertirse automáticamente en TEACHER");
        assertEquals("$2a$12$eX4mpleHashPlaceholderForMigrationTestOnly1234567890", migratedTeacher.getPasswordHash(), "La contraseña no debe ser modificada");

        // 3. Comprobar a nivel de base de datos relacional la columna role
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT role FROM teachers WHERE id = ?", testId);
        assertEquals("TEACHER", row.get("role"));

        // 4. Comprobar que la restricción CHECK impide roles inválidos a nivel SQL
        assertThrows(Exception.class, () ->
                jdbcTemplate.update("UPDATE teachers SET role = 'SUPERUSER_INVALID' WHERE id = ?", testId)
        );
    }
}
