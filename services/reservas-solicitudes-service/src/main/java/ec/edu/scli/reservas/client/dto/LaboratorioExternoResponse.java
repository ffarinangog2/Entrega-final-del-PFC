package ec.edu.scli.reservas.client.dto;

import java.util.UUID;

/** Respuesta externa con la disponibilidad base de un laboratorio. */
public record LaboratorioExternoResponse(
        UUID laboratorioId,
        boolean existe,
        boolean activo,
        String estado,
        Integer capacidad
) {
}
