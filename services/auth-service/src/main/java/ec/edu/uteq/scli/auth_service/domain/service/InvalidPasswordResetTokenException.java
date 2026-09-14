package ec.edu.uteq.scli.auth_service.domain.service;
public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() { super("El enlace de recuperación no es válido o ha expirado"); }
}
