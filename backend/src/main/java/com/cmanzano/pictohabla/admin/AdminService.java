package com.cmanzano.pictohabla.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Servicio de operaciones exclusivas para administradores.
 * Protegido mediante Spring Method Security. Un usuario con rol TEACHER
 * recibe automáticamente un error 403 (AccessDeniedException).
 */
@Service
public class AdminService {

    @PreAuthorize("hasRole('ADMIN')")
    public String executeAdminOperation() {
        return "Operación administrativa ejecutada con éxito";
    }
}
