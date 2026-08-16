package ec.edu.scli.academico.dto.bloque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BloqueRequest(

        @NotNull(message = "El campus es obligatorio")
        UUID campusId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre

) {
}
