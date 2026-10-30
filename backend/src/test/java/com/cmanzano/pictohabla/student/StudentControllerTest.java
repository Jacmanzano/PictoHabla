package com.cmanzano.pictohabla.student;

import com.cmanzano.pictohabla.classroom.Classroom;
import com.cmanzano.pictohabla.security.SecurityUser;
import com.cmanzano.pictohabla.student.dto.CreateStudentRequest;
import com.cmanzano.pictohabla.student.dto.StudentResponse;
import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherStatus;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    private SecurityUser teacherUser;
    private UUID teacherId;
    private UUID classroomId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        classroomId = UUID.randomUUID();
        Teacher teacher = new Teacher(teacherId, "profe@colegio.es", "hash", "Carlos", TeacherStatus.ACTIVE);
        teacherUser = new SecurityUser(teacher);
    }

    @Test
    void addStudent_ValidRequest_ReturnsCreatedWithPseudonym() throws Exception {
        CreateStudentRequest request = new CreateStudentRequest("Lucas", null);
        StudentResponse response = new StudentResponse(
                UUID.randomUUID(), classroomId, "ALU-9F2B8C", "Lucas", true, Instant.now()
        );

        when(studentService.addStudentToClassroom(eq(teacherId), eq(classroomId), any(CreateStudentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/classrooms/{classroomId}/students", classroomId)
                        .with(user(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pseudonymCode").value("ALU-9F2B8C"))
                .andExpect(jsonPath("$.data.nickname").value("Lucas"));
    }

    @Test
    void getStudents_Authenticated_ReturnsList() throws Exception {
        StudentResponse student1 = new StudentResponse(
                UUID.randomUUID(), classroomId, "ALU-111111", "Lucas", true, Instant.now()
        );
        StudentResponse student2 = new StudentResponse(
                UUID.randomUUID(), classroomId, "ALU-222222", "Marta", true, Instant.now()
        );

        when(studentService.getStudentsInClassroom(teacherId, classroomId)).thenReturn(List.of(student1, student2));

        mockMvc.perform(get("/api/v1/classrooms/{classroomId}/students", classroomId)
                        .with(user(teacherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].pseudonymCode").value("ALU-111111"));

        verify(studentService).getStudentsInClassroom(teacherId, classroomId);
    }
}
