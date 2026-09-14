package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface EquipoJpaRepository
        extends JpaRepository<EquipoEntity, UUID>,
        JpaSpecificationExecutor<EquipoEntity> {

    List<EquipoEntity> findByLaboratorioId(UUID laboratorioId);

    boolean existsByCodigoInventario(String codigoInventario);

    boolean existsByCodigoInventarioAndIdNot(String codigoInventario, UUID id);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, UUID id);

    boolean existsByTipoEquipoId(UUID tipoEquipoId);

    boolean existsByLaboratorioId(UUID laboratorioId);
}
