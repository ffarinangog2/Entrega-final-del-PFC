package ec.edu.scli.academico.domain.exception;

/**
 * Se lanza cuando se viola una regla de negocio del dominio académico
 * (ej. fecha de fin anterior a fecha de inicio, o una relación con una
 * entidad que no existe). La capa presentation (GlobalExceptionHandler)
 * la traduce a un 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}
