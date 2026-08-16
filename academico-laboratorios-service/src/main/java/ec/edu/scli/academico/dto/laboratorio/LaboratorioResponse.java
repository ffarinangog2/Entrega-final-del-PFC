package ec.edu.scli.academico.dto.laboratorio;

import ec.edu.scli.academico.enums.EstadoLaboratorio;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LaboratorioResponse(

        UUID id,

        UUID pisoId,

        String codigo,

        String nombre,

        Integer capacidad,

        String descripcion,

        EstadoLaboratorio estado,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
