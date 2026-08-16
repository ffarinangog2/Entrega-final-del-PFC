package ec.edu.scli.usuarios.dto.tecnico;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TecnicoResponse(

        UUID id,

        UUID perfilId,

        String codigoTecnico,

        String especialidad,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}