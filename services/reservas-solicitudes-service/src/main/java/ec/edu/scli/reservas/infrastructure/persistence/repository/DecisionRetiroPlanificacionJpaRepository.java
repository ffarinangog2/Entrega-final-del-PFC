package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.DecisionRetiroPlanificacionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DecisionRetiroPlanificacionJpaRepository
        extends JpaRepository<DecisionRetiroPlanificacionJpaEntity, UUID> {
    List<DecisionRetiroPlanificacionJpaEntity> findBySolicitudRetiroId(UUID solicitudRetiroId);
    Optional<DecisionRetiroPlanificacionJpaEntity> findBySolicitudRetiroIdAndPisoId(
            UUID solicitudRetiroId, UUID pisoId);
}
