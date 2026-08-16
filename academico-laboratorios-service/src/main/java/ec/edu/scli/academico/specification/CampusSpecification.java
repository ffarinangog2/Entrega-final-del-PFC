package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Campus;
import org.springframework.data.jpa.domain.Specification;

public final class CampusSpecification {

    private CampusSpecification() {
    }

    public static Specification<Campus> codigoContiene(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%");
        };
    }

    public static Specification<Campus> nombreContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<Campus> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
