package ec.edu.scli.usuarios.dto.administrador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdministradorRequest(

        @NotNull(message = "El perfilId es obligatorio")
        UUID perfilId,

        @NotBlank(message = "El código de administrador es obligatorio")
        @Size(
                max = 30,
                message = "El código de administrador no puede superar 30 caracteres"
        )
        String codigoAdministrador,

        @Size(
                max = 100,
                message = "El cargo no puede superar 100 caracteres"
        )
        String cargo,

        /*
         * Referencia UUID externa al microservicio
         * de laboratorios.
         */
        UUID pisoId,

        Boolean activo

) {
}