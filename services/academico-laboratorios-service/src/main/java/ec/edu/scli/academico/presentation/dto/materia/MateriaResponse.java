package ec.edu.scli.academico.presentation.dto.materia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MateriaResponse(

        UUID id,

        UUID carreraId,

        String codigo,

        String nombre,

        Integer numeroHoras,

        Integer nivel,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
    public MateriaResponse(UUID id, UUID carreraId, String codigo, String nombre,
            Integer numeroHoras, Boolean activo, OffsetDateTime creadoEn, OffsetDateTime actualizadoEn) {
        this(id, carreraId, codigo, nombre, numeroHoras, null, activo, creadoEn, actualizadoEn);
    }
}
