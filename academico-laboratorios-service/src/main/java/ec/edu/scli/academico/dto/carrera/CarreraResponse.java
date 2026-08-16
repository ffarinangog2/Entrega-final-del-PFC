package ec.edu.scli.academico.dto.carrera;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CarreraResponse(

        UUID id,

        UUID facultadId,

        String codigo,

        String nombre,

        String descripcion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
