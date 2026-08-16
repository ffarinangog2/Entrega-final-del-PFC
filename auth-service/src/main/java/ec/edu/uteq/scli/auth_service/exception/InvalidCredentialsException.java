package ec.edu.uteq.scli.auth_service.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}