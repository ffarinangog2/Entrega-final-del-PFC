package ec.edu.scli.academico.dto.piso;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PisoRequest(

        @NotNull(message = "El bloque es obligatorio")
        UUID bloqueId,

        @NotNull(message = "El número de piso es obligatorio")
        @Min(value = 0, message = "El número de piso no puede ser negativo")
        Integer numero,

        @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
        String descripcion

) {
}
