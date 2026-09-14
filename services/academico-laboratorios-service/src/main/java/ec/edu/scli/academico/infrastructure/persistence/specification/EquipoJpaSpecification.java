package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EquipoJpaSpecification {

    private EquipoJpaSpecification() {
    }

    public static Specification<EquipoEntity> tieneLaboratorio(UUID laboratorioId) {
        return (root, query, cb) -> {
            if (laboratorioId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("laboratorioId"), laboratorioId);
        };
    }

    public static Specification<EquipoEntity> tieneEstadoEquipo(EstadoEquipo estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<EquipoEntity> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
