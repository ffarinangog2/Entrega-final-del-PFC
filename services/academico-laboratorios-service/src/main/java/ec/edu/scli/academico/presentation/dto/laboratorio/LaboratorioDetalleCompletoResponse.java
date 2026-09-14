package ec.edu.scli.academico.presentation.dto.laboratorio;

import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;

import java.util.List;

/**
 * Ficha completa de un laboratorio: su ubicación (piso, bloque, campus)
 * y su inventario de equipos, agregados en una sola respuesta.
 * Construido por LaboratorioDetalleFacade (patrón Facade).
 */
public record LaboratorioDetalleCompletoResponse(

        LaboratorioResponse laboratorio,

        PisoResponse piso,

        BloqueResponse bloque,

        CampusResponse campus,

        List<EquipoResponse> equipos

) {
}