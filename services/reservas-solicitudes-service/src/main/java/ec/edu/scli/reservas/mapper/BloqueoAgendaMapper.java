package ec.edu.scli.reservas.mapper;

import ec.edu.scli.reservas.dto.response.BloqueoAgendaResponse;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import org.springframework.stereotype.Component;

/** Convierte bloqueos de agenda a sus representaciones de respuesta. */
@Component
public class BloqueoAgendaMapper {

    public BloqueoAgendaResponse toResponse(BloqueoAgenda bloqueo) {
        return new BloqueoAgendaResponse(
                bloqueo.getId(),
                bloqueo.getLaboratorioId(),
                bloqueo.getFecha(),
                bloqueo.getHoraInicio(),
                bloqueo.getHoraFin(),
                bloqueo.getMotivo(),
                bloqueo.getCreadoPor(),
                bloqueo.getActivo(),
                bloqueo.getCreadoEn(),
                bloqueo.getVersion()
        );
    }
}
