package ec.edu.uteq.scli.auth_service.application.service;
public interface PasswordResetMailService { void sendResetLink(String email, String link); }
