package ec.edu.scli.academico.dto.materia;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MateriaRequest(

        @NotNull(message = "La carrera es obligatoria")
        UUID carreraId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotNull(message = "El número de horas es obligatorio")
        @Min(value = 0, message = "El número de horas no puede ser negativo")
        Integer numeroHoras

) {
}
