package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.HorarioAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface HorarioAcademicoRepository
        extends JpaRepository<HorarioAcademico, UUID>,
        JpaSpecificationExecutor<HorarioAcademico> {

    List<HorarioAcademico> findByDocenteId(UUID docenteId);

    List<HorarioAcademico> findByLaboratorioId(UUID laboratorioId);

    boolean existsByMateriaId(UUID materiaId);

    boolean existsByPeriodoLectivoId(UUID periodoLectivoId);

    boolean existsByLaboratorioId(UUID laboratorioId);
}
