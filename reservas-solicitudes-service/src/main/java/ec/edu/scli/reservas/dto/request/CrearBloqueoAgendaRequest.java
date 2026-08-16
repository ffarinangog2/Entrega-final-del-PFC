package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Datos requeridos para bloquear un intervalo de la agenda. */
public record CrearBloqueoAgendaRequest(
        @NotNull UUID laboratorioId,
        @NotNull @FutureOrPresent LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotBlank @Size(max = 500) String motivo
) {
    /** Valida que el intervalo del bloqueo tenga una duración positiva. */
    @AssertTrue(message = "La hora final debe ser mayor que la hora inicial")
    public boolean isHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
