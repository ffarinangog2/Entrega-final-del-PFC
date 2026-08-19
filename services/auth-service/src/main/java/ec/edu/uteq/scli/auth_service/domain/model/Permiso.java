package ec.edu.uteq.scli.auth_service.domain.model;

import java.util.UUID;

/** Permiso de negocio, independiente de la persistencia. */
public record Permiso(UUID id, String codigo, String nombre, String descripcion, boolean activo) {
}
