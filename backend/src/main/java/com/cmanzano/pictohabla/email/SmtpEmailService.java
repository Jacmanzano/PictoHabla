package com.cmanzano.pictohabla.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos transaccionales para verificación de cuentas y restablecimiento de contraseñas.
 * Si el servidor SMTP no está configurado en las variables de entorno, reporta su indisponibilidad
 * y emite una advertencia estructurada sin exponer tokens ni enlaces en los registros.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@pictohabla.local}")
    private String fromEmail;

    @Value("${app.mail.base-url:http://localhost:8080}")
    private String baseUrl;

    public SmtpEmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean isAvailable() {
        return mailSender != null;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        if (!isAvailable()) {
            log.warn("Servidor SMTP no configurado. El correo de verificación para '{}' no se ha enviado.", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("PictoHabla — Verificación de cuenta de profesor");
            message.setText(
                    "Hola " + fullName + ",\n\n" +
                    "Gracias por registrarte en PictoHabla. Para activar tu cuenta, accede al siguiente enlace:\n" +
                    baseUrl + "/api/v1/auth/verify-email?token=" + token + "\n\n" +
                    "Este enlace caducará en 24 horas.\n\n" +
                    "— El equipo de PictoHabla"
            );
            mailSender.send(message);
            log.info("Correo de verificación enviado a {}", toEmail);
        } catch (Exception e) {
            log.error("Error al enviar correo de verificación a {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        if (!isAvailable()) {
            log.warn("Servidor SMTP no configurado. El correo de recuperación para '{}' no se ha enviado.", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("PictoHabla — Restablecimiento de contraseña");
            message.setText(
                    "Hola " + fullName + ",\n\n" +
                    "Hemos recibido una solicitud para restablecer tu contraseña. Accede al siguiente enlace para elegir una nueva contraseña:\n" +
                    baseUrl + "/api/v1/auth/reset-password?token=" + token + "\n\n" +
                    "Este enlace es de un solo uso y caduca en 1 hora. Si no realizaste esta solicitud, puedes ignorar este mensaje.\n\n" +
                    "— El equipo de PictoHabla"
            );
            mailSender.send(message);
            log.info("Correo de restablecimiento de contraseña enviado a {}", toEmail);
        } catch (Exception e) {
            log.error("Error al enviar correo de restablecimiento a {}: {}", toEmail, e.getMessage());
        }
    }
}
