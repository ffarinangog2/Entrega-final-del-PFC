package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface BloqueJpaRepository
        extends JpaRepository<BloqueEntity, UUID>,
        JpaSpecificationExecutor<BloqueEntity> {

    List<BloqueEntity> findByCampusId(UUID campusId);

    boolean existsByCampusIdAndCodigo(UUID campusId, String codigo);

    boolean existsByCampusIdAndCodigoAndIdNot(UUID campusId, String codigo, UUID id);
}
