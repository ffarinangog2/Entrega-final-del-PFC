package ec.edu.scli.academico.dto.facultad;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FacultadResponse(

        UUID id,

        String codigo,

        String nombre,

        String descripcion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
