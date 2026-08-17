package ec.edu.scli.reservas.exception;

import java.time.Instant;

/** Representa la respuesta estándar para los errores expuestos por la API. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
