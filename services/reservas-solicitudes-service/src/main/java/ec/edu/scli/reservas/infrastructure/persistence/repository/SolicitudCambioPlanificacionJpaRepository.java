package ec.edu.scli.reservas.infrastructure.persistence.repository;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudCambioPlanificacionJpaEntity; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SolicitudCambioPlanificacionJpaRepository extends JpaRepository<SolicitudCambioPlanificacionJpaEntity,UUID>{ List<SolicitudCambioPlanificacionJpaEntity> findByPlanificacionIdOrderByCreadaEnDesc(UUID id); boolean existsByBloqueIdAndEstado(UUID bloqueId,ec.edu.scli.reservas.domain.model.EstadoSolicitudCambio estado); }
