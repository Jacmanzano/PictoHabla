package com.cmanzano.pictohabla.classroom;

import com.cmanzano.pictohabla.classroom.dto.ClassroomRequest;
import com.cmanzano.pictohabla.classroom.dto.ClassroomResponse;
import com.cmanzano.pictohabla.common.exception.ResourceNotFoundException;
import com.cmanzano.pictohabla.security.CustomUserDetailsService;
import com.cmanzano.pictohabla.security.JwtService;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassroomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassroomService classroomService;

    private SecurityUser teacherUserA;
    private SecurityUser teacherUserB;
    private UUID teacherIdA;
    private UUID teacherIdB;

    @BeforeEach
    void setUp() {
        teacherIdA = UUID.randomUUID();
        Teacher teacherA = new Teacher(teacherIdA, "profeA@colegio.es", "hash", "Profesor A", TeacherStatus.ACTIVE);
        teacherUserA = new SecurityUser(teacherA);

        teacherIdB = UUID.randomUUID();
        Teacher teacherB = new Teacher(teacherIdB, "profeB@colegio.es", "hash", "Profesor B", TeacherStatus.ACTIVE);
        teacherUserB = new SecurityUser(teacherB);
    }

    @Test
    void createClassroom_AuthenticatedTeacher_ReturnsCreated() throws Exception {
        ClassroomRequest request = new ClassroomRequest("Aula Azul", "Aula de infantil 5 años");
        ClassroomResponse response = new ClassroomResponse(
                UUID.randomUUID(), teacherIdA, "Aula Azul", "Aula de infantil 5 años", true, Instant.now(), Instant.now()
        );

        when(classroomService.createClassroom(eq(teacherIdA), any(ClassroomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/classrooms")
                        .with(user(teacherUserA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Aula Azul"));
    }

    @Test
    void getClassrooms_ReturnsOnlyTeacherOwnClassrooms() throws Exception {
        ClassroomResponse classroomA = new ClassroomResponse(
                UUID.randomUUID(), teacherIdA, "Aula A1", "Desc", true, Instant.now(), Instant.now()
        );

        when(classroomService.getClassroomsForTeacher(teacherIdA)).thenReturn(List.of(classroomA));

        mockMvc.perform(get("/api/v1/classrooms")
                        .with(user(teacherUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Aula A1"));

        verify(classroomService).getClassroomsForTeacher(teacherIdA);
        verify(classroomService, never()).getClassroomsForTeacher(teacherIdB);
    }

    @Test
    void getClassroomById_BelongingToAnotherTeacher_ReturnsNotFound() throws Exception {
        UUID classroomIdA = UUID.randomUUID();

        // Cuando el profesor B intenta acceder al aula del profesor A, el servicio lanza ResourceNotFoundException
        when(classroomService.getClassroomById(teacherIdB, classroomIdA))
                .thenThrow(new ResourceNotFoundException("Aula no encontrada"));

        mockMvc.perform(get("/api/v1/classrooms/{id}", classroomIdA)
                        .with(user(teacherUserB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Aula no encontrada"));
    }

    @Test
    void createClassroom_BlankName_ReturnsBadRequest() throws Exception {
        ClassroomRequest invalidRequest = new ClassroomRequest("", "Sin nombre");

        mockMvc.perform(post("/api/v1/classrooms")
                        .with(user(teacherUserA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getClassrooms_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
