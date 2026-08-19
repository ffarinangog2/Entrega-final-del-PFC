package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Bloque;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class BloqueSpecification {

    private BloqueSpecification() {
    }

    public static Specification<Bloque> tieneCampus(UUID campusId) {
        return (root, query, cb) -> {
            if (campusId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("campusId"), campusId);
        };
    }

    public static Specification<Bloque> nombreContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<Bloque> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
