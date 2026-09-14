package ec.edu.scli.usuarios.presentation.dto.estudiante;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EstudianteResponse(

        UUID id,

        UUID perfilId,

        String matricula,

        UUID carreraId,

        Integer semestre,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}