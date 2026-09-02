package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionPlanificacionPisoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevisionPlanificacionPisoJpaRepository extends JpaRepository<RevisionPlanificacionPisoJpaEntity, UUID> {
    List<RevisionPlanificacionPisoJpaEntity> findByPlanificacionId(UUID planificacionId);
    Optional<RevisionPlanificacionPisoJpaEntity> findByPlanificacionIdAndPisoId(UUID planificacionId, UUID pisoId);
}
