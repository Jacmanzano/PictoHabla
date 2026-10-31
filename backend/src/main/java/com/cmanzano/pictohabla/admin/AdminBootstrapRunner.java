package com.cmanzano.pictohabla.admin;

import com.cmanzano.pictohabla.teacher.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.Scanner;

/**
 * Runner de línea de comandos para la ejecución one-shot del bootstrap de administrador.
 * Se activa ÚNICAMENTE si la propiedad 'pictohabla.bootstrap.admin=true' está presente.
 * En ejecución normal de la API web permanece completamente inactivo.
 */
@Component
@ConditionalOnProperty(name = "pictohabla.bootstrap.admin", havingValue = "true")
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService adminBootstrapService;

    public AdminBootstrapRunner(AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @Override
    public void run(String... args) {
        System.out.println("==============================================================================");
        System.out.println(" PictoHabla — Asistente de Creación Segura del Primer Administrador");
        System.out.println("==============================================================================");

        if (adminBootstrapService.isAdminAlreadyPresent()) {
            System.err.println("[ERROR] Ya existe un administrador registrado en el sistema.");
            System.err.println("El mecanismo de bootstrap de un solo uso está desactivado permanentemente.");
            throw new IllegalStateException("El proceso de bootstrap ya fue completado previamente.");
        }

        String email = System.getenv("ADMIN_EMAIL");
        String fullName = System.getenv("ADMIN_FULL_NAME");
        String password = System.getenv("ADMIN_PASSWORD");

        // Soporte adicional para argumentos de línea de comandos o propiedades del sistema
        for (String arg : args) {
            if (arg.startsWith("--admin.email=") || arg.startsWith("--pictohabla.bootstrap.email=")) {
                email = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("--admin.fullName=") || arg.startsWith("--admin.name=") || arg.startsWith("--pictohabla.bootstrap.name=")) {
                fullName = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("--admin.password=") || arg.startsWith("--pictohabla.bootstrap.password=")) {
                password = arg.substring(arg.indexOf('=') + 1);
            }
        }

        if (email == null || email.isBlank()) {
            email = System.getProperty("admin.email", System.getProperty("pictohabla.bootstrap.email"));
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = System.getProperty("admin.fullName", System.getProperty("pictohabla.bootstrap.name"));
        }
        if (password == null || password.isBlank()) {
            password = System.getProperty("admin.password", System.getProperty("pictohabla.bootstrap.password"));
        }

        Console console = System.console();
        Scanner stdinScanner = new Scanner(System.in);

        if (email == null || email.isBlank()) {
            if (console != null) {
                email = console.readLine("Correo electrónico del Administrador: ");
            } else if (stdinScanner.hasNextLine()) {
                email = stdinScanner.nextLine().trim();
            }
        }

        if (fullName == null || fullName.isBlank()) {
            if (console != null) {
                fullName = console.readLine("Nombre completo del Administrador: ");
            } else if (stdinScanner.hasNextLine()) {
                fullName = stdinScanner.nextLine().trim();
            }
        }

        if (password == null || password.isBlank()) {
            if (console != null) {
                char[] passChars = console.readPassword("Contraseña del Administrador (oculta): ");
                if (passChars != null) {
                    password = new String(passChars);
                }
                char[] confirmChars = console.readPassword("Confirmar contraseña: ");
                String confirm = confirmChars != null ? new String(confirmChars) : "";
                if (!password.equals(confirm)) {
                    System.err.println("[ERROR] Las contraseñas no coinciden. Operación abortada.");
                    throw new IllegalArgumentException("Las contraseñas no coinciden");
                }
            } else if (stdinScanner.hasNextLine()) {
                password = stdinScanner.nextLine();
            }
        }

        try {
            Teacher admin = adminBootstrapService.bootstrapAdmin(email, password, fullName);
            System.out.println("------------------------------------------------------------------------------");
            System.out.println("[OK] Primer administrador creado exitosamente.");
            System.out.println("     - ID:    " + admin.getId());
            System.out.println("     - Email: " + admin.getEmail());
            System.out.println("     - Rol:   " + admin.getRole());
            System.out.println("==============================================================================");
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo crear el administrador: " + e.getMessage());
            throw new IllegalStateException("Fallo en el proceso de bootstrap del administrador: " + e.getMessage(), e);
        }
    }
}
