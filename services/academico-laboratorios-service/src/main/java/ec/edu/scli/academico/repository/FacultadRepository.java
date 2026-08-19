package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FacultadRepository
        extends JpaRepository<Facultad, UUID>,
        JpaSpecificationExecutor<Facultad> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
