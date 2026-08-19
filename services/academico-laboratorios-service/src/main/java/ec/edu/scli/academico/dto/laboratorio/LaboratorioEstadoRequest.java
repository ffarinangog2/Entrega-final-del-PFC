package ec.edu.scli.academico.dto.laboratorio;

import ec.edu.scli.academico.enums.EstadoLaboratorio;
import jakarta.validation.constraints.NotNull;

public record LaboratorioEstadoRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoLaboratorio estado

) {
}
