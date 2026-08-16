package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Equipo;
import ec.edu.scli.academico.enums.EstadoEquipo;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EquipoSpecification {

    private EquipoSpecification() {
    }

    public static Specification<Equipo> tieneLaboratorio(UUID laboratorioId) {
        return (root, query, cb) -> {
            if (laboratorioId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("laboratorioId"), laboratorioId);
        };
    }

    public static Specification<Equipo> tieneTipoEquipo(UUID tipoEquipoId) {
        return (root, query, cb) -> {
            if (tipoEquipoId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tipoEquipoId"), tipoEquipoId);
        };
    }

    public static Specification<Equipo> tieneEstadoEquipo(EstadoEquipo estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<Equipo> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
