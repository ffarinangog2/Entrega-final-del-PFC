package ec.edu.scli.academico.infrastructure.persistence.specification;

import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CarreraJpaSpecification {

    private CarreraJpaSpecification() {
    }

    public static Specification<CarreraEntity> codigoContiene(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%");
        };
    }

    public static Specification<CarreraEntity> nombreContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<CarreraEntity> tieneFacultad(UUID facultadId) {
        return (root, query, cb) -> {
            if (facultadId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("facultadId"), facultadId);
        };
    }

    public static Specification<CarreraEntity> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
