package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.infrastructure.persistence.entity.ParticipanteUsoLaboratorioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ParticipanteUsoLaboratorioJpaRepository extends JpaRepository<ParticipanteUsoLaboratorioJpaEntity, UUID> {
    List<ParticipanteUsoLaboratorioJpaEntity> findBySesionIdOrderByEstudiantePerfilId(UUID sesionId);
    Optional<ParticipanteUsoLaboratorioJpaEntity> findBySesionIdAndEstudiantePerfilId(UUID sesionId, UUID perfilId);
}
