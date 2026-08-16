package ec.edu.scli.academico.dto.bloque;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BloqueResponse(

        UUID id,

        UUID campusId,

        String codigo,

        String nombre,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
