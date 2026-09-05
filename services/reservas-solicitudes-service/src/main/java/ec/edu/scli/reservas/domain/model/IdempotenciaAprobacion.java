package ec.edu.scli.reservas.domain.model;

import java.util.UUID;

/** Registro que vincula una clave de idempotencia con una aprobación y su resultado. */
public record IdempotenciaAprobacion(
        String clave,
        String operacion,
        UUID solicitudId,
        UUID reservaId
) {
}
