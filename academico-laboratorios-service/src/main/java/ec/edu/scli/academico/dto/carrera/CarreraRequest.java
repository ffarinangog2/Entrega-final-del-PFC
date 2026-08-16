package ec.edu.scli.academico.dto.carrera;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CarreraRequest(

        @NotNull(message = "La facultad es obligatoria")
        UUID facultadId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion

) {
}
