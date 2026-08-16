package ec.edu.scli.reservas.dto.response;

import ec.edu.scli.reservas.enums.EstadoSolicitud;

import java.time.Instant;
import java.util.UUID;

/** Representación de un cambio en el historial de una solicitud. */
public record HistorialSolicitudResponse(
        UUID id,
        UUID solicitudId,
        EstadoSolicitud estadoAnterior,
        EstadoSolicitud estadoNuevo,
        UUID usuarioAccionId,
        String comentario,
        Instant fechaHora
) {
}
