package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CampusRepository
        extends JpaRepository<Campus, UUID>,
        JpaSpecificationExecutor<Campus> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
