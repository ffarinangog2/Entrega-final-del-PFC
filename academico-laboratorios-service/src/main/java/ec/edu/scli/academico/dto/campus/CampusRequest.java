package ec.edu.scli.academico.dto.campus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CampusRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion

) {
}
