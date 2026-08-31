package ec.edu.scli.reservas.infrastructure.persistence.repository;
import ec.edu.scli.reservas.infrastructure.persistence.entity.DispositivoNotificacionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DispositivoNotificacionRepository extends JpaRepository<DispositivoNotificacionJpaEntity,UUID> {
    Optional<DispositivoNotificacionJpaEntity> findByToken(String token);
    List<DispositivoNotificacionJpaEntity> findByPerfilIdAndActivoTrue(UUID perfilId);
}
