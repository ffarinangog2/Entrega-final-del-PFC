package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface LaboratorioJpaRepository
        extends JpaRepository<LaboratorioEntity, UUID>,
        JpaSpecificationExecutor<LaboratorioEntity> {

    List<LaboratorioEntity> findByPisoId(UUID pisoId);

    List<LaboratorioEntity> findByEstadoAndActivoTrue(EstadoLaboratorio estado);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByPisoId(UUID pisoId);
}
