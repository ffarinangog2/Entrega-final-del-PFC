package ec.edu.scli.reservas.infrastructure.persistence.repository;
import ec.edu.scli.reservas.infrastructure.persistence.entity.RevisionSolicitudCambioJpaEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RevisionSolicitudCambioJpaRepository extends JpaRepository<RevisionSolicitudCambioJpaEntity,UUID>{ List<RevisionSolicitudCambioJpaEntity> findBySolicitudId(UUID id); Optional<RevisionSolicitudCambioJpaEntity> findBySolicitudIdAndPisoId(UUID solicitudId,UUID pisoId); }
