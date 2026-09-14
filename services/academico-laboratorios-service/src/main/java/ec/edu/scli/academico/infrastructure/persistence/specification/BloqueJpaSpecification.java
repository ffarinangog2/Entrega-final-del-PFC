package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class BloqueJpaSpecification {

    private BloqueJpaSpecification() {
    }

    public static Specification<BloqueEntity> tieneCampus(UUID campusId) {
        return (root, query, cb) -> {
            if (campusId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("campusId"), campusId);
        };
    }

    public static Specification<BloqueEntity> nombreContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<BloqueEntity> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
