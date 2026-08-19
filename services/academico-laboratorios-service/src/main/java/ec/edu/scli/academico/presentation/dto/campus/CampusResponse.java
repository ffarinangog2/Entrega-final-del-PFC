package ec.edu.scli.academico.presentation.dto.campus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CampusResponse(

        UUID id,

        String codigo,

        String nombre,

        String direccion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
