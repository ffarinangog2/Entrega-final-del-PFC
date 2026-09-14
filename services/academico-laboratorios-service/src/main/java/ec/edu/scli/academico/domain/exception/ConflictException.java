package ec.edu.scli.academico.domain.exception;

/**
 * Se lanza cuando una operación viola una restricción de unicidad del
 * dominio académico (ej. un código que ya existe). La capa presentation
 * (GlobalExceptionHandler) la traduce a un 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
