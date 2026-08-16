package ec.edu.scli.usuarios.dto.perfil;

import jakarta.validation.constraints.NotNull;

public record PerfilEstadoRequest(

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}