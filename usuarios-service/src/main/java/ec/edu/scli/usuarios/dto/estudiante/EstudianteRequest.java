package ec.edu.scli.usuarios.dto.estudiante;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EstudianteRequest(

        @NotNull(message = "El perfilId es obligatorio")
        UUID perfilId,

        @NotBlank(message = "La matrícula es obligatoria")
        @Size(max = 30, message = "La matrícula no puede superar 30 caracteres")
        String matricula,

        UUID carreraId,

        @Min(value = 1, message = "El semestre mínimo es 1")
        @Max(value = 20, message = "El semestre máximo es 20")
        Integer semestre,

        Boolean activo

) {
}