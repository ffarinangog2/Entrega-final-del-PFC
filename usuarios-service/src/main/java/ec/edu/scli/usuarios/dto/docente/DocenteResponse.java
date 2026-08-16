package ec.edu.scli.usuarios.dto.docente;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocenteResponse(

        UUID id,

        UUID perfilId,

        String codigoDocente,

        String tituloAcademico,

        String departamento,

        String tipoContrato,

        String dedicacion,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}