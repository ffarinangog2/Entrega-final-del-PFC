package ec.edu.scli.reservas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Datos para aprobar una solicitud de reserva. */
public record AprobarSolicitudRequest(
        @NotNull UUID responsableId,
        @Size(max = 2000) String comentario
) {
}
