package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.admin.AdminBootstrapRunner;
import com.cmanzano.pictohabla.admin.AdminService;
import com.cmanzano.pictohabla.auth.AuthService;
import com.cmanzano.pictohabla.auth.dto.AuthResponse;
import com.cmanzano.pictohabla.auth.dto.LoginRequest;
import com.cmanzano.pictohabla.auth.dto.RegisterRequest;
import com.cmanzano.pictohabla.classroom.Classroom;
import com.cmanzano.pictohabla.classroom.ClassroomRepository;
import com.cmanzano.pictohabla.classroom.ClassroomService;
import com.cmanzano.pictohabla.classroom.dto.ClassroomRequest;
import com.cmanzano.pictohabla.common.exception.ForbiddenException;
import com.cmanzano.pictohabla.common.exception.ResourceNotFoundException;
import com.cmanzano.pictohabla.teacher.Role;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import com.cmanzano.pictohabla.teacher.TeacherService;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.cmanzano.pictohabla.teacher.dto.UpdateTeacherRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherRoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM admin_bootstrap_metadata");
        classroomRepository.deleteAll();
        teacherRepository.deleteAll();
    }

    @Test
    void newTeacher_ReceivesRoleTeacher() {
        RegisterRequest request = new RegisterRequest("nuevo.profe@colegio.es", "Password123", "Nuevo Profesor");
        TeacherResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.TEACHER, response.role());

        Teacher saved = teacherRepository.findById(response.id()).orElseThrow();
        assertEquals(Role.TEACHER, saved.getRole());
    }

    @Test
    void manipulatedRegistration_DoesNotAllowAdmin() throws Exception {
        // Enviar JSON manipulado con role=ADMIN en el cuerpo de la petición
        String manipulatedJson = """
                {
                    "email": "hacker@colegio.es",
                    "password": "Password123",
                    "fullName": "Intento Escalada",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manipulatedJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("hacker@colegio.es"))
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        Teacher saved = teacherRepository.findByEmailIgnoreCase("hacker@colegio.es").orElseThrow();
        assertEquals(Role.TEACHER, saved.getRole(), "El rol guardado en la base de datos debe ser estrictamente TEACHER");
    }

    @Test
    void teacher_CannotChangeRole() throws Exception {
        Teacher teacher = new Teacher(
                UUID.randomUUID(),
                "profe.fijo@colegio.es",
                passwordEncoder.encode("Password123"),
                "Profesor Fijo",
                TeacherStatus.ACTIVE,
                Role.TEACHER
        );
        teacherRepository.save(teacher);
        SecurityUser securityUser = new SecurityUser(teacher);

        // Intentar inyectar role=ADMIN en la actualización de perfil
        String updateJson = """
                {
                    "fullName": "Profesor Renombrado",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(patch("/api/v1/teachers/me")
                        .with(user(securityUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Profesor Renombrado"))
                .andExpect(jsonPath("$.data.role").value("TEACHER"));

        Teacher updated = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("Profesor Renombrado", updated.getFullName());
        assertEquals(Role.TEACHER, updated.getRole(), "El rol debe permanecer inalterado como TEACHER");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacher_CannotAccessAdminOperations() {
        // Un usuario con rol TEACHER debe ser rechazado por Method Security
        assertThrows(AccessDeniedException.class, () ->
                adminService.executeAdminOperation()
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_CanAccessAdminOperations() {
        // Un usuario con rol ADMIN debe poder ejecutar la operación
        String result = adminService.executeAdminOperation();
        assertNotNull(result);
        assertTrue(result.contains("éxito"));
    }

    @Test
    void securityUser_AdminHasRoleAdminAuthority() {
        Teacher admin = new Teacher(
                UUID.randomUUID(),
                "admin@colegio.es",
                "hash",
                "Admin",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        );
        SecurityUser user = new SecurityUser(admin);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER")));
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    void teachers_MaintainAccessToOwnClassrooms() {
        Teacher teacherA = teacherRepository.save(new Teacher(
                UUID.randomUUID(), "profea@colegio.es", "hash", "Profe A", TeacherStatus.ACTIVE, Role.TEACHER
        ));

        Classroom classroomA = classroomRepository.save(new Classroom(
                UUID.randomUUID(), teacherA, "Aula A", "Descripción A"
        ));

        // Profe A puede acceder a su propia aula
        assertDoesNotThrow(() ->
                classroomService.getClassroomById(teacherA.getId(), classroomA.getId())
        );
        assertEquals(1, classroomService.getClassroomsForTeacher(teacherA.getId()).size());
    }

    @Test
    void teacher_CannotAccessOtherTeacherClassrooms() {
        Teacher teacherA = teacherRepository.save(new Teacher(
                UUID.randomUUID(), "profea2@colegio.es", "hash", "Profe A2", TeacherStatus.ACTIVE, Role.TEACHER
        ));
        Teacher teacherB = teacherRepository.save(new Teacher(
                UUID.randomUUID(), "profeb2@colegio.es", "hash", "Profe B2", TeacherStatus.ACTIVE, Role.TEACHER
        ));

        Classroom classroomA = classroomRepository.save(new Classroom(
                UUID.randomUUID(), teacherA, "Aula A2", "Descripción A2"
        ));

        // Profe B intenta acceder al aula de Profe A -> debe lanzar ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                classroomService.getClassroomById(teacherB.getId(), classroomA.getId())
        );
    }

    @Test
    void loginAndRefresh_PreserveAccountRole() {
        // 1. Probar flujo con TEACHER
        Teacher teacher = teacherRepository.save(new Teacher(
                UUID.randomUUID(),
                "login.profe@colegio.es",
                passwordEncoder.encode("Password123"),
                "Profesor Login",
                TeacherStatus.ACTIVE,
                Role.TEACHER
        ));
        teacher.setEmailVerified(true);
        teacherRepository.save(teacher);

        AuthResponse teacherAuth = authService.login(new LoginRequest("login.profe@colegio.es", "Password123"), null);
        assertNotNull(teacherAuth.accessToken());
        assertEquals(Role.TEACHER, teacherAuth.teacher().role());
        assertEquals(Role.TEACHER, jwtService.extractRole(teacherAuth.accessToken()));

        AuthResponse teacherRefreshed = authService.refreshToken(teacherAuth.refreshToken(), null);
        assertEquals(Role.TEACHER, teacherRefreshed.teacher().role());
        assertEquals(Role.TEACHER, jwtService.extractRole(teacherRefreshed.accessToken()));

        // 2. Probar flujo con ADMIN
        Teacher admin = teacherRepository.save(new Teacher(
                UUID.randomUUID(),
                "login.admin@colegio.es",
                passwordEncoder.encode("AdminPass123"),
                "Administrador Login",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        ));
        admin.setEmailVerified(true);
        teacherRepository.save(admin);

        AuthResponse adminAuth = authService.login(new LoginRequest("login.admin@colegio.es", "AdminPass123"), null);
        assertNotNull(adminAuth.accessToken());
        assertEquals(Role.ADMIN, adminAuth.teacher().role());
        assertEquals(Role.ADMIN, jwtService.extractRole(adminAuth.accessToken()));

        AuthResponse adminRefreshed = authService.refreshToken(adminAuth.refreshToken(), null);
        assertEquals(Role.ADMIN, adminRefreshed.teacher().role());
        assertEquals(Role.ADMIN, jwtService.extractRole(adminRefreshed.accessToken()));
    }

    @Test
    void bootstrap_DoesNotOpenHttpEndpoint() {
        // Comprobar que AdminBootstrapRunner no es un RestController ni un Controller
        assertFalse(AdminBootstrapRunner.class.isAnnotationPresent(RestController.class));
        assertFalse(AdminBootstrapRunner.class.isAnnotationPresent(Controller.class));
    }

    @Test
    void adminUser_IsForbiddenFromAccessingClassroomEndpoints() throws Exception {
        Teacher admin = teacherRepository.save(new Teacher(
                UUID.randomUUID(),
                "admin.blocked@colegio.es",
                passwordEncoder.encode("AdminPass123"),
                "Admin Bloqueado",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        ));
        SecurityUser adminSecurityUser = new SecurityUser(admin);

        // Intento de listar aulas siendo ADMIN -> HTTP 403 Forbidden por @PreAuthorize("hasRole('TEACHER')")
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/classrooms")
                        .with(user(adminSecurityUser)))
                .andExpect(status().isForbidden());

        // Intento de crear aula siendo ADMIN -> HTTP 403 Forbidden
        String createJson = """
                {
                    "name": "Aula Intento Admin",
                    "description": "No permitida para ADMIN"
                }
                """;
        mockMvc.perform(post("/api/v1/classrooms")
                        .with(user(adminSecurityUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isForbidden());

        // Intento de consultar alumnos de un aula siendo ADMIN -> HTTP 403 Forbidden
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/classrooms/" + UUID.randomUUID() + "/students")
                        .with(user(adminSecurityUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUser_IsForbiddenFromCreatingClassroomInService() {
        Teacher admin = teacherRepository.save(new Teacher(
                UUID.randomUUID(),
                "admin.service.blocked@colegio.es",
                passwordEncoder.encode("AdminPass123"),
                "Admin Service Bloqueado",
                TeacherStatus.ACTIVE,
                Role.ADMIN
        ));

        // ClassroomService debe lanzar ForbiddenException si el usuario no tiene rol TEACHER
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                classroomService.createClassroom(admin.getId(), new ClassroomRequest("Aula Invalida", "Desc"))
        );
        assertTrue(ex.getMessage().contains("Solo los usuarios con rol de profesor"));
    }

    @Test
    void tokenWithForgedAdminClaim_ForUserWithTeacherRoleInDb_IsRejectedByJwtAuthenticationFilter() throws Exception {
        // Crear profesor en base de datos con rol TEACHER
        Teacher realTeacher = teacherRepository.save(new Teacher(
                UUID.randomUUID(),
                "teacher.legit@colegio.es",
                passwordEncoder.encode("Password123"),
                "Profesor Legitimo",
                TeacherStatus.ACTIVE,
                Role.TEACHER
        ));
        realTeacher.setEmailVerified(true);
        teacherRepository.save(realTeacher);

        // Forjar un token JWT firmado donde se declara role=ADMIN para este profesor
        String forgedToken = jwtService.generateAccessToken(
                realTeacher.getId(),
                realTeacher.getEmail(),
                realTeacher.getFullName(),
                Role.ADMIN
        );

        // Petición con el token forjado a un endpoint protegido
        // JwtAuthenticationFilter detecta discrepancia (Token dice ADMIN, BD dice TEACHER)
        // Por seguridad, descarta la autenticación y no establece el contexto -> HTTP 401 Unauthorized
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/teachers/me")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }
}
