package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para cancelar una solicitud de reserva. */
public record CancelarSolicitudRequest(
        @NotBlank @Size(max = 2000) String comentario
) {
}
