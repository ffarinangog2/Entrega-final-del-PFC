package ec.edu.scli.reservas.infrastructure.persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*; import ec.edu.scli.reservas.infrastructure.persistence.entity.RegistroAsistenciaJpaEntity;
public interface RegistroAsistenciaJpaRepository extends JpaRepository<RegistroAsistenciaJpaEntity,UUID>{ boolean existsBySesionIdAndEstudianteId(UUID sesionId,UUID estudianteId); List<RegistroAsistenciaJpaEntity> findByEstudianteId(UUID estudianteId); List<RegistroAsistenciaJpaEntity> findBySesionId(UUID sesionId); }
