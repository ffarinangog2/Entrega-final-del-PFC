package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.HorarioAcademicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface HorarioAcademicoJpaRepository
        extends JpaRepository<HorarioAcademicoEntity, UUID>,
        JpaSpecificationExecutor<HorarioAcademicoEntity> {

    List<HorarioAcademicoEntity> findByDocenteId(UUID docenteId);

    List<HorarioAcademicoEntity> findByLaboratorioId(UUID laboratorioId);
}
