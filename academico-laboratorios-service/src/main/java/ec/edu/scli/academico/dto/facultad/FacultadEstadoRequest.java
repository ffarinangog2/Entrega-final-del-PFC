package ec.edu.scli.academico.dto.facultad;

import jakarta.validation.constraints.NotNull;

public record FacultadEstadoRequest(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}
