package ec.edu.scli.usuarios.presentation.dto.usuarios;

import java.util.UUID;

public record AuthUsuarioResponse(UUID id, UUID perfilId, String username, String email,
        String rol, boolean activo) {
}
