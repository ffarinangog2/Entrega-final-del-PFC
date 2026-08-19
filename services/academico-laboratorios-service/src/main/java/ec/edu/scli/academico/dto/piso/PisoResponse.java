package ec.edu.scli.academico.dto.piso;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PisoResponse(

        UUID id,

        UUID bloqueId,

        Integer numero,

        String descripcion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
