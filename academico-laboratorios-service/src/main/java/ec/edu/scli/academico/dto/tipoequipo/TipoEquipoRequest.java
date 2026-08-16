package ec.edu.scli.academico.dto.tipoequipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoEquipoRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion

) {
}
