package ec.edu.scli.reservas.infrastructure.persistence.repository;
import ec.edu.scli.reservas.infrastructure.persistence.entity.NotificacionInternaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface NotificacionInternaJpaRepository extends JpaRepository<NotificacionInternaJpaEntity,UUID>{
 List<NotificacionInternaJpaEntity> findTop50ByPerfilIdOrderByCreadaEnDesc(UUID perfilId);
 Page<NotificacionInternaJpaEntity> findByPerfilId(UUID perfilId, Pageable pageable);
 long countByPerfilIdAndLeidaFalse(UUID perfilId);
 Optional<NotificacionInternaJpaEntity> findByIdAndPerfilId(UUID id,UUID perfilId);
 boolean existsByClaveEvento(String claveEvento);
 List<NotificacionInternaJpaEntity> findByPerfilIdAndLeidaFalse(UUID perfilId);
}
