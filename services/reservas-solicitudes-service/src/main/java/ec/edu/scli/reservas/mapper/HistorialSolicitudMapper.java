package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.entity.HistorialSolicitud;
import org.springframework.stereotype.Component;

/** Convierte historiales de solicitud a sus representaciones de respuesta. */
@Component
public class HistorialSolicitudMapper {

    public HistorialSolicitudResponse toResponse(HistorialSolicitud historial) {
        return new HistorialSolicitudResponse(
                historial.getId(),
                historial.getSolicitud() != null ? historial.getSolicitud().getId() : null,
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getUsuarioAccionId(),
                historial.getComentario(),
                historial.getFechaHora()
        );
    }
}
