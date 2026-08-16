package ec.edu.scli.reservas.dto.response;

import ec.edu.scli.reservas.enums.EstadoSolicitud;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Representación de una solicitud de reserva. */
public record SolicitudReservaResponse(
        UUID id,
        UUID solicitanteId,
        UUID docenteId,
        UUID laboratorioId,
        UUID materiaId,
        UUID periodoLectivoId,
        LocalDate fechaReserva,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer numeroParticipantes,
        String motivo,
        String observacion,
        EstadoSolicitud estado,
        UUID reservaId,
        Instant creadaEn,
        Instant actualizadaEn,
        Long version
) {
}
