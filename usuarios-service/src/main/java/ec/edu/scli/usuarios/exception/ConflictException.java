package ec.edu.scli.usuarios.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}