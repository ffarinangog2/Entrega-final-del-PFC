package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class PisoJpaSpecification {

    private PisoJpaSpecification() {
    }

    public static Specification<PisoEntity> tieneBloque(UUID bloqueId) {
        return (root, query, cb) -> {
            if (bloqueId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("bloqueId"), bloqueId);
        };
    }

    public static Specification<PisoEntity> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
