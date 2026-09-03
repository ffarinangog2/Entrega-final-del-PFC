package ec.edu.uteq.scli.auth_service.presentation.dto;

import java.util.UUID;

public record AdminUsuarioResponse(
        UUID id,
        UUID perfilId,
        String username,
        String email,
        String rol,
        boolean activo) {
}
