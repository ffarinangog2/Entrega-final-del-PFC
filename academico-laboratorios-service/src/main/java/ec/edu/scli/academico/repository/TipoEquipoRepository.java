package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.TipoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TipoEquipoRepository
        extends JpaRepository<TipoEquipo, UUID>,
        JpaSpecificationExecutor<TipoEquipo> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
