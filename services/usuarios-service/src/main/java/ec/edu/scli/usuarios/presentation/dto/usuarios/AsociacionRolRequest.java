package ec.edu.scli.usuarios.presentation.dto.usuarios;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AsociacionRolRequest(
        @NotBlank String rol,
        UUID pisoId,
        UUID carreraId) {
}
