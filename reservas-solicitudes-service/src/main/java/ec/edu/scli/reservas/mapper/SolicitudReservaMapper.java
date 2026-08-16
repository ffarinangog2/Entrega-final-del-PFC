package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.entity.SolicitudReserva;
import org.springframework.stereotype.Component;

/** Convierte solicitudes de reserva a sus representaciones de respuesta. */
@Component
public class SolicitudReservaMapper {

    public SolicitudReservaResponse toResponse(SolicitudReserva solicitud) {
        return new SolicitudReservaResponse(
                solicitud.getId(),
                solicitud.getSolicitanteId(),
                solicitud.getDocenteId(),
                solicitud.getLaboratorioId(),
                solicitud.getMateriaId(),
                solicitud.getPeriodoLectivoId(),
                solicitud.getFechaReserva(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin(),
                solicitud.getNumeroParticipantes(),
                solicitud.getMotivo(),
                solicitud.getObservacion(),
                solicitud.getEstado(),
                solicitud.getReserva() != null ? solicitud.getReserva().getId() : null,
                solicitud.getCreadaEn(),
                solicitud.getActualizadaEn(),
                solicitud.getVersion()
        );
    }
}
