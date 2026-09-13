package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.domain.model.EstadoSolicitudRetiro;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudRetiroPlanificacionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudRetiroPlanificacionJpaRepository
        extends JpaRepository<SolicitudRetiroPlanificacionJpaEntity, UUID> {
    List<SolicitudRetiroPlanificacionJpaEntity> findByPlanificacionIdOrderByCreadaEnDesc(UUID planificacionId);
    Optional<SolicitudRetiroPlanificacionJpaEntity> findByPlanificacionIdAndEstado(
            UUID planificacionId, EstadoSolicitudRetiro estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudRetiroPlanificacionJpaEntity> findLockedById(UUID id);
}
