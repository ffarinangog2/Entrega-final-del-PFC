package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.presentation.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.domain.model.HistorialSolicitud;
import org.springframework.stereotype.Component;

/** Convierte historiales de solicitud a sus representaciones de respuesta. */
@Component
public class HistorialSolicitudMapper {

    public HistorialSolicitudResponse toResponse(HistorialSolicitud historial) {
        return new HistorialSolicitudResponse(
                historial.getId(),
                historial.getSolicitudId(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getUsuarioAccionId(),
                historial.getComentario(),
                historial.getFechaHora()
        );
    }
}
