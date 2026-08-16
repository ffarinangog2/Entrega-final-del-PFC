package ec.edu.scli.usuarios.dto.tecnico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TecnicoRequest(

        @NotNull(message = "El perfilId es obligatorio")
        UUID perfilId,

        @NotBlank(message = "El código técnico es obligatorio")
        @Size(
                max = 30,
                message = "El código técnico no puede superar 30 caracteres"
        )
        String codigoTecnico,

        @Size(
                max = 100,
                message = "La especialidad no puede superar 100 caracteres"
        )
        String especialidad,

        Boolean activo

) {
}