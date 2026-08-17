package ec.edu.scli.reservas.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Identidad autenticada extraída del token emitido por auth-service.
 *
 * <p>El nombre del principal es el perfilId porque Reservas utiliza los
 * identificadores de perfil del dominio de Usuarios como referencias externas.
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
