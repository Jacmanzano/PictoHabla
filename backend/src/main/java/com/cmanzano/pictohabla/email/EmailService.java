package com.cmanzano.pictohabla.email;

public interface EmailService {
    boolean isAvailable();
    void sendVerificationEmail(String toEmail, String fullName, String token);
    void sendPasswordResetEmail(String toEmail, String fullName, String token);
}
