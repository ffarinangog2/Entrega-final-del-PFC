package ec.edu.scli.reservas.presentation.dto.response;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;

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
