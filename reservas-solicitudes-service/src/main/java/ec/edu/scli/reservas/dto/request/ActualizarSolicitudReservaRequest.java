package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Datos editables de una solicitud de reserva. */
public record ActualizarSolicitudReservaRequest(
        @NotNull UUID docenteId,
        @NotNull UUID laboratorioId,
        @NotNull UUID materiaId,
        @NotNull UUID periodoLectivoId,
        @NotNull @FutureOrPresent LocalDate fechaReserva,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotNull @Positive Integer numeroParticipantes,
        @NotBlank @Size(max = 500) String motivo,
        @Size(max = 2000) String observacion
) {
    /** Valida que el intervalo actualizado tenga una duración positiva. */
    @AssertTrue(message = "La hora final debe ser mayor que la hora inicial")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
