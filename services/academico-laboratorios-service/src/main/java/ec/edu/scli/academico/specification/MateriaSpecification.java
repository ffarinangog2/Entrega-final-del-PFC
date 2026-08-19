package ec.edu.scli.academico.specification;

import ec.edu.scli.academico.entity.Materia;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class MateriaSpecification {

    private MateriaSpecification() {
    }

    public static Specification<Materia> codigoContiene(String codigo) {
        return (root, query, cb) -> {
            if (codigo == null || codigo.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%");
        };
    }

    public static Specification<Materia> nombreContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<Materia> tieneCarrera(UUID carreraId) {
        return (root, query, cb) -> {
            if (carreraId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("carreraId"), carreraId);
        };
    }

    public static Specification<Materia> tieneEstado(Boolean activo) {
        return (root, query, cb) -> {
            if (activo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activo"), activo);
        };
    }
}
