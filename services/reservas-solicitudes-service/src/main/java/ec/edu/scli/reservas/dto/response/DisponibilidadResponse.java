package ec.edu.scli.reservas.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Resultado de la consulta de disponibilidad de un laboratorio. */
public record DisponibilidadResponse(
        UUID laboratorioId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean disponible,
        String motivo
) {
}
