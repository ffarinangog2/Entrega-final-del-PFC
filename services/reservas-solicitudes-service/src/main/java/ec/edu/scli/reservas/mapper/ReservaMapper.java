package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.domain.model.Reserva;
import org.springframework.stereotype.Component;

/** Convierte reservas a sus representaciones de respuesta. */
@Component
public class ReservaMapper {

    public ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getSolicitudId(),
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
