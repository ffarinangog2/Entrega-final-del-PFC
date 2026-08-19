package ec.edu.scli.academico.dto.materia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MateriaResponse(

        UUID id,

        UUID carreraId,

        String codigo,

        String nombre,

        Integer numeroHoras,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}
