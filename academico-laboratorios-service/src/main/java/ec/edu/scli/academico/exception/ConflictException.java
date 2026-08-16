package ec.edu.scli.academico.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
