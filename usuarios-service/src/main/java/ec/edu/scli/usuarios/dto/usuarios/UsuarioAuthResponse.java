package ec.edu.scli.usuarios.dto.usuarios;

import java.util.UUID;

public record UsuarioAuthResponse(
        UUID usuarioId,
        String correo,
        String passwordHash,
        boolean activo,
        UUID perfilId) {
}