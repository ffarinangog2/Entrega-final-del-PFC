package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Piso;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class PisoSpecification {

    private PisoSpecification() {
    }

    public static Specification<Piso> tieneBloque(UUID bloqueId) {
        return (root, query, cb) -> {
            if (bloqueId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("bloqueId"), bloqueId);
        };
    }

    public static Specification<Piso> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
