package ec.edu.scli.reservas.presentation.exception;

/** Indica que un recurso solicitado no existe y debe representarse como HTTP 404. */
public class ResourceNotFoundException extends IllegalArgumentException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
