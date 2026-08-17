package ec.edu.scli.reservas.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Representación de un bloqueo de agenda de laboratorio. */
public record BloqueoAgendaResponse(
        UUID id,
        UUID laboratorioId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String motivo,
        UUID creadoPor,
        Boolean activo,
        Instant creadoEn,
        Long version
) {
}
