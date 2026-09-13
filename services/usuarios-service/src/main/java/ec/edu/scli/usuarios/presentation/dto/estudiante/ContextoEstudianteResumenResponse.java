package ec.edu.scli.usuarios.presentation.dto.estudiante;

import java.util.UUID;

public record ContextoEstudianteResumenResponse(
        UUID perfilId,
        UUID estudianteId,
        UUID carreraId,
        UUID periodoId,
        Integer nivel,
        boolean activo
) {
}
