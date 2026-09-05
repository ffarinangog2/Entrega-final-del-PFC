package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.IncidenteJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidenteSpringDataRepository extends JpaRepository<IncidenteJpaEntity, UUID> {
    Page<IncidenteJpaEntity> findByReportanteId(UUID reportanteId, Pageable pageable);
}
