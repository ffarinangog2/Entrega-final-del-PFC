package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionAgregadaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanificacionAgregadaJpaRepository extends JpaRepository<PlanificacionAgregadaJpaEntity, UUID> {
    Optional<PlanificacionAgregadaJpaEntity> findByCarreraIdAndPeriodoId(UUID carreraId, UUID periodoId);
    List<PlanificacionAgregadaJpaEntity> findByCarreraIdOrderByCreadaEnDesc(UUID carreraId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PlanificacionAgregadaJpaEntity> findLockedById(UUID id);
}
