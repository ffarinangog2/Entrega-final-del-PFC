package ec.edu.scli.reservas.domain.model;

import java.util.UUID;

/** Vincula una creación de solicitud con actor, payload y resultado persistente. */
public record IdempotenciaCreacionSolicitud(
        String clave,
        String operacion,
        UUID actorId,
        String payloadHash,
        UUID solicitudId) {
}
