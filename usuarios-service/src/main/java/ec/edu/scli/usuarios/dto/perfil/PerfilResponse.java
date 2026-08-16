package ec.edu.scli.usuarios.dto.perfil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PerfilResponse(

        UUID id,

        String identificacion,

        String nombres,

        String apellidos,


        String emailInstitucional,

        String emailPersonal,

        String telefono,

        String direccion,

        LocalDate fechaNacimiento,

        String fotoUrl,

        Boolean activo,

        OffsetDateTime creadoEn,

        OffsetDateTime actualizadoEn

) {
}