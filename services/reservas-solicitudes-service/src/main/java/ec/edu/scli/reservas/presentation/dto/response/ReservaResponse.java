package ec.edu.scli.reservas.presentation.dto.response;

import ec.edu.scli.reservas.domain.model.EstadoReserva;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Representación de una reserva confirmada. */
public record ReservaResponse(
        UUID id,
        UUID solicitudId,
        UUID laboratorioId,
        UUID responsableId,
        LocalDate fechaReserva,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoReserva estado,
        String codigoReserva,
        Instant creadaEn,
        Instant actualizadaEn,
        Long version
) {
}
