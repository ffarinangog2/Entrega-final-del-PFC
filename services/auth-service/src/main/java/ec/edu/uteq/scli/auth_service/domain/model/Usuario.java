package ec.edu.uteq.scli.auth_service.domain.model;

import java.util.Set;
import java.util.UUID;

/** Agregado de autenticación, sin dependencias de infraestructura. */
public record Usuario(UUID id, UUID perfilId, String username, String email, String passwordHash,
                      boolean activo, boolean cuentaBloqueada, Set<Rol> roles) {
    public Usuario {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
