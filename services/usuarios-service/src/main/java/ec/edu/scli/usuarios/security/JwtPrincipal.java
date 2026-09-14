package ec.edu.scli.usuarios.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Identidad autenticada extraída del token emitido por auth-service.
 *
 * <p>El nombre del principal es el perfilId porque Usuarios expone sus
 * recursos (perfiles) identificados por ese mismo id de dominio.
 */
public record JwtPrincipal(
        UUID usuarioAuthId,
        UUID perfilId,
        String username
) implements Principal {

    @Override
    public String getName() {
        return perfilId.toString();
    }
}
