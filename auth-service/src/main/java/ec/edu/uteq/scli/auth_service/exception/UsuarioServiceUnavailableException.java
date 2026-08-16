package ec.edu.uteq.scli.auth_service.exception;

public class UsuarioServiceUnavailableException extends RuntimeException {
    public UsuarioServiceUnavailableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}