package ec.edu.scli.reservas.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Elemento unificado para presentar reservas y bloqueos en una agenda. */
public record AgendaItemResponse(
        UUID id,
        String tipo,
        UUID laboratorioId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado,
        String descripcion
) {
}
