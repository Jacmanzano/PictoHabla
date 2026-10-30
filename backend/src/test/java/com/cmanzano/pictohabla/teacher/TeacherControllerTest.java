package com.cmanzano.pictohabla.teacher;

import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.dto.TeacherResponse;
import com.cmanzano.pictohabla.teacher.dto.UpdateTeacherRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeacherService teacherService;

    private SecurityUser teacherUser;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        Teacher teacher = new Teacher(teacherId, "profe@colegio.es", "hash", "Carlos Manzano", TeacherStatus.ACTIVE);
        teacherUser = new SecurityUser(teacher);
    }

    @Test
    void getProfile_Authenticated_ReturnsProfile() throws Exception {
        TeacherResponse response = new TeacherResponse(
                teacherId, "profe@colegio.es", "Carlos Manzano", TeacherStatus.ACTIVE, true, Instant.now()
        );

        when(teacherService.getTeacherProfile(teacherId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teachers/me")
                        .with(user(teacherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profe@colegio.es"))
                .andExpect(jsonPath("$.data.fullName").value("Carlos Manzano"));

        verify(teacherService).getTeacherProfile(teacherId);
    }

    @Test
    void updateProfile_ValidRequest_ReturnsUpdated() throws Exception {
        UpdateTeacherRequest request = new UpdateTeacherRequest("Carlos M. Editado");
        TeacherResponse response = new TeacherResponse(
                teacherId, "profe@colegio.es", "Carlos M. Editado", TeacherStatus.ACTIVE, true, Instant.now()
        );

        when(teacherService.updateTeacherProfile(eq(teacherId), any(UpdateTeacherRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/teachers/me")
                        .with(user(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Carlos M. Editado"));

        verify(teacherService).updateTeacherProfile(eq(teacherId), any(UpdateTeacherRequest.class));
    }
}
