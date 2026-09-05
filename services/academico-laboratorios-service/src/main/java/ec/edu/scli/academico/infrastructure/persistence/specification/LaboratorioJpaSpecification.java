package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import org.springframework.data.jpa.domain.Specification;

public final class LaboratorioJpaSpecification {

    private LaboratorioJpaSpecification() {
    }

    public static Specification<LaboratorioEntity> nombreOCodigoContiene(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) {
                return cb.conjunction();
            }
            String patron = "%" + texto.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("codigo")), patron)
            );
        };
    }

    public static Specification<LaboratorioEntity> tieneEstadoLaboratorio(EstadoLaboratorio estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<LaboratorioEntity> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
