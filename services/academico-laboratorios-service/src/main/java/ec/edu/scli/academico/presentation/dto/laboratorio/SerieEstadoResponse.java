package ec.edu.scli.academico.presentation.dto.laboratorio;

import java.util.List;

/** Serie temporal de un estado de laboratorio (ej. todos los puntos de "OCUPADO"). */
public record SerieEstadoResponse(

        String estado,

        List<PuntoSerieResponse> puntos

) {
}