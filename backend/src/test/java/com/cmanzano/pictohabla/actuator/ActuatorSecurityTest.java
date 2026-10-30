package com.cmanzano.pictohabla.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "management.server.port=0",
        "management.server.address=127.0.0.1",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=never"
})
class ActuatorSecurityTest {

    @LocalServerPort
    private int mainServerPort;

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Actuator en puerto de gestión independiente (8081 en prod, random en test) responde 200 UP sin detalles")
    void actuatorHealth_OnManagementPort_ReturnsUpWithoutSensitiveDetails() {
        assertNotEquals(mainServerPort, managementPort, "El puerto de Actuator debe ser independiente del puerto de la API");

        String managementUrl = "http://127.0.0.1:" + managementPort + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(managementUrl, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"UP\""));
        // show-details: never -> No debe contener detalles de componentes internos
        assertFalse(response.getBody().contains("db"), "No debe exponer detalles de la base de datos");
        assertFalse(response.getBody().contains("diskSpace"), "No debe exponer detalles de espacio en disco");
    }

    @Test
    @DisplayName("Endpoints no autorizados de Actuator (info, env, beans) no son accesibles públicamente")
    void actuatorOtherEndpoints_AreNotExposed() {
        String infoUrl = "http://127.0.0.1:" + managementPort + "/actuator/info";
        ResponseEntity<String> infoResponse = restTemplate.getForEntity(infoUrl, String.class);
        assertTrue(infoResponse.getStatusCode() == HttpStatus.NOT_FOUND || infoResponse.getStatusCode() == HttpStatus.UNAUTHORIZED,
                "/actuator/info no debe ser accesible públicamente");

        String envUrl = "http://127.0.0.1:" + managementPort + "/actuator/env";
        ResponseEntity<String> envResponse = restTemplate.getForEntity(envUrl, String.class);
        assertTrue(envResponse.getStatusCode() == HttpStatus.NOT_FOUND || envResponse.getStatusCode() == HttpStatus.UNAUTHORIZED,
                "/actuator/env no debe ser accesible públicamente");
    }

    @Test
    @DisplayName("El puerto principal de la API NO expone /actuator/health (devuelve 404)")
    void mainApiPort_DoesNotExposeActuatorEndpoints() {
        String mainApiActuatorUrl = "http://127.0.0.1:" + mainServerPort + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(mainApiActuatorUrl, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "El puerto principal (8080 en prod) no debe exponer Actuator");
    }
}
