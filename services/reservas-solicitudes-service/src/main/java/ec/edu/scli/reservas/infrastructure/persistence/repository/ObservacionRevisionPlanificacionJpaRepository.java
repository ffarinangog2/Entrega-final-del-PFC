package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.ObservacionRevisionPlanificacionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ObservacionRevisionPlanificacionJpaRepository
        extends JpaRepository<ObservacionRevisionPlanificacionJpaEntity, UUID> {
    List<ObservacionRevisionPlanificacionJpaEntity> findByRevisionId(UUID revisionId);
    void deleteByRevisionId(UUID revisionId);
}
