package ec.edu.scli.reservas.infrastructure.persistence.repository;
import ec.edu.scli.reservas.domain.model.EstadoSesionAsistencia;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SesionAsistenciaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
public interface SesionAsistenciaJpaRepository extends JpaRepository<SesionAsistenciaJpaEntity,UUID>{
 Optional<SesionAsistenciaJpaEntity> findByIdAndDocenteId(UUID id,UUID docenteId);
 Optional<SesionAsistenciaJpaEntity> findFirstByReservaIdAndEstado(UUID reservaId, EstadoSesionAsistencia estado);
 List<SesionAsistenciaJpaEntity> findByEstado(EstadoSesionAsistencia estado);
}
