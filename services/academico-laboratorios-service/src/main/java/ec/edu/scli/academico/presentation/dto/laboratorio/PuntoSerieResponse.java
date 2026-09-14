package ec.edu.scli.academico.presentation.dto.laboratorio;

import java.time.OffsetDateTime;

/** Un punto de la serie temporal: instante + valor en ese instante. */
public record PuntoSerieResponse(

        OffsetDateTime instante,

        double valor

) {
}