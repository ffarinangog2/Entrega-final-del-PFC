package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Bloque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface BloqueRepository
        extends JpaRepository<Bloque, UUID>,
        JpaSpecificationExecutor<Bloque> {

    List<Bloque> findByCampusId(UUID campusId);

    boolean existsByCampusIdAndCodigo(UUID campusId, String codigo);

    boolean existsByCampusIdAndCodigoAndIdNot(UUID campusId, String codigo, UUID id);
}
