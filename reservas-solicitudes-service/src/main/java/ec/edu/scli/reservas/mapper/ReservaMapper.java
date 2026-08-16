package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.entity.Reserva;
import org.springframework.stereotype.Component;

/** Convierte reservas a sus representaciones de respuesta. */
@Component
public class ReservaMapper {

    public ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getSolicitud() != null ? reserva.getSolicitud().getId() : null,
                reserva.getLaboratorioId(),
                reserva.getResponsableId(),
                reserva.getFechaReserva(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                reserva.getEstado(),
                reserva.getCodigoReserva(),
                reserva.getCreadaEn(),
                reserva.getActualizadaEn(),
                reserva.getVersion()
        );
    }
}
