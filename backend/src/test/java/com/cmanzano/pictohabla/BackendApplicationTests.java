package com.cmanzano.pictohabla;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring Boot se inicialice correctamente,
        // validando la inyección de beans, repositorios y migraciones de Flyway.
    }
}
