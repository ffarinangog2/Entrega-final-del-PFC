package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class LaboratorioSpecification {

    private LaboratorioSpecification() {
    }

    public static Specification<Laboratorio> tienePiso(UUID pisoId) {
        return (root, query, cb) -> {
            if (pisoId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("pisoId"), pisoId);
        };
    }

    public static Specification<Laboratorio> nombreOCodigoContiene(String texto) {
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

    public static Specification<Laboratorio> tieneEstadoLaboratorio(EstadoLaboratorio estado) {
        return (root, query, cb) -> {
            if (estado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<Laboratorio> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
