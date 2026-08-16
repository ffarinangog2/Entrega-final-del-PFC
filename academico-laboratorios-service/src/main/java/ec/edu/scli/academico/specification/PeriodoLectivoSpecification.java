package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.PeriodoLectivo;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import org.springframework.data.jpa.domain.Specification;

public final class PeriodoLectivoSpecification {

    private PeriodoLectivoSpecification() {
    }

    public static Specification<PeriodoLectivo> codigoContiene(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%");
        };
    }

    public static Specification<PeriodoLectivo> tieneEstado(EstadoPeriodo estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }
}
