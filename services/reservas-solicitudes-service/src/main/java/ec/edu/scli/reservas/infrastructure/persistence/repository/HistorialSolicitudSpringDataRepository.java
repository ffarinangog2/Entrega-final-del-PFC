package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.HistorialSolicitudJpaEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HistorialSolicitudSpringDataRepository extends JpaRepository<HistorialSolicitudJpaEntity, UUID> {
    Page<HistorialSolicitudJpaEntity> findBySolicitudIdOrderByFechaHoraAsc(UUID solicitudId, Pageable pageable);
}
