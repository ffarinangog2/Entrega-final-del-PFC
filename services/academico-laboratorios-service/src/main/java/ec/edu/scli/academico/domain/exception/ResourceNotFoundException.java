package ec.edu.scli.academico.domain.exception;

/**
 * Se lanza cuando se busca un recurso del dominio académico que no existe.
 * Vive en domain porque expresa una violación de una invariante del
 * negocio ("este id debe existir"), no un detalle de transporte HTTP.
 * La capa presentation (GlobalExceptionHandler) la traduce a un 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
