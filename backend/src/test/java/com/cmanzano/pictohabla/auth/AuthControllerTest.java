package com.cmanzano.pictohabla.auth;

import com.cmanzano.pictohabla.auth.dto.AuthResponse;
import com.cmanzano.pictohabla.auth.dto.LoginRequest;
import com.cmanzano.pictohabla.auth.dto.RegisterRequest;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_ValidData_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("profe@colegio.es", "Password123", "Profesor Carlos");
        TeacherResponse teacherResponse = new TeacherResponse(
                UUID.randomUUID(), "profe@colegio.es", "Profesor Carlos", TeacherStatus.ACTIVE, true, Instant.now()
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(teacherResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profe@colegio.es"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("email-no-valido", "Password123", "Profesor");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("profe@colegio.es", "123", "Profesor");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_ValidCredentials_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("profe@colegio.es", "Password123");
        TeacherResponse teacherResponse = new TeacherResponse(
                UUID.randomUUID(), "profe@colegio.es", "Profesor Carlos", TeacherStatus.ACTIVE, true, Instant.now()
        );
        AuthResponse authResponse = AuthResponse.of("access.token.jwt", 900, teacherResponse, "refresh.token.uuid");

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.data.teacher.email").value("profe@colegio.es"));
    }

    @Test
    void getMe_Authenticated_ReturnsProfile() throws Exception {
        Teacher teacher = new Teacher(UUID.randomUUID(), "profe@colegio.es", "hash", "Profesor Carlos", TeacherStatus.ACTIVE);
        SecurityUser securityUser = new SecurityUser(teacher);
        TeacherResponse teacherResponse = TeacherResponse.fromDomain(teacher);

        when(authService.getCurrentTeacher(any(SecurityUser.class))).thenReturn(teacherResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(user(securityUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Profesor Carlos"));
    }

    @Test
    void getMe_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
