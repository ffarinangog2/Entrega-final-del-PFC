package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import org.springframework.data.jpa.domain.Specification;

public final class PeriodoLectivoJpaSpecification {

    private PeriodoLectivoJpaSpecification() {
    }

    public static Specification<PeriodoLectivoEntity> codigoContiene(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%");
        };
    }
}
