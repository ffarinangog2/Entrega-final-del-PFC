package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para rechazar una solicitud de reserva. */
public record RechazarSolicitudRequest(
        @NotBlank @Size(max = 2000) String comentario
) {
}
