package ec.edu.scli.reservas.client.dto;

import java.util.UUID;

/** Respuesta externa que indica si un recurso identificado existe. */
public record ExisteExternoResponse(
        UUID id,
        boolean existe
) {
}
