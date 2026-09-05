package ec.edu.scli.usuarios.presentation.dto.usuarios;

import java.util.UUID;

public record AuthUsuarioCreateRequest(UUID perfilId, String username, String email,
        String passwordInicial, String rol) {
}
