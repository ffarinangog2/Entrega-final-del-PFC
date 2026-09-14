package ec.edu.scli.academico.security;

import java.util.UUID;

public record JwtPrincipal(UUID usuarioAuthId, UUID perfilId, String username) {
}
