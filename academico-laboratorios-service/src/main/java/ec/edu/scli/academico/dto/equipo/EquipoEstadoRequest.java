package ec.edu.scli.academico.dto.equipo;

import ec.edu.scli.academico.enums.EstadoEquipo;
import jakarta.validation.constraints.NotNull;

public record EquipoEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoEquipo estado

) {
}
