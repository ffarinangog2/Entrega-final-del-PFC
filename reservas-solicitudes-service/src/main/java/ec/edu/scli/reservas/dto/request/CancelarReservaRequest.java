package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para cancelar una reserva confirmada. */
public record CancelarReservaRequest(
        @NotBlank @Size(max = 2000) String motivo
) {
}
