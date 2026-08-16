package ec.edu.scli.academico.dto.tipoequipo;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TipoEquipoResponse(

        UUID id,

        String codigo,

        String nombre,

        String descripcion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
