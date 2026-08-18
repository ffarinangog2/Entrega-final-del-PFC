package ec.edu.scli.usuarios.presentation.dto.administrador;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdministradorResponse(

        UUID id,

        UUID perfilId,

        String codigoAdministrador,

        String cargo,

        UUID pisoId,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}