package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContextoAcademicoEstudianteRepository extends JpaRepository<ContextoAcademicoEstudianteEntity, UUID> {
    List<ContextoAcademicoEstudianteEntity> findByEstudianteIdOrderByCreadoEnDesc(UUID estudianteId);
    Optional<ContextoAcademicoEstudianteEntity> findFirstByEstudianteIdAndActivoTrueOrderByCreadoEnDesc(UUID estudianteId);
    Optional<ContextoAcademicoEstudianteEntity> findByEstudianteIdAndPeriodoId(UUID estudianteId, UUID periodoId);
    List<ContextoAcademicoEstudianteEntity> findByCarreraIdAndPeriodoIdAndNivelAndActivoTrue(UUID carreraId,UUID periodoId,Integer nivel);
}
