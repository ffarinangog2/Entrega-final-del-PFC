package ec.edu.uteq.scli.auth_service.domain.model;

import java.util.Set;
import java.util.UUID;

/** Rol de negocio y sus permisos asociados. */
public record Rol(UUID id, String codigo, String nombre, String descripcion, boolean activo, Set<Permiso> permisos) {
    public Rol {
        permisos = permisos == null ? Set.of() : Set.copyOf(permisos);
    }
}
