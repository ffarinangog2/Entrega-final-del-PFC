package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.HorarioAcademico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HorarioAcademicoRepositoryPort {

    HorarioAcademico guardar(HorarioAcademico horario);

    Optional<HorarioAcademico> buscarPorId(UUID id);

    List<HorarioAcademico> buscarTodos();

    List<HorarioAcademico> buscarPorDocente(UUID docenteId);

    List<HorarioAcademico> buscarPorLaboratorio(UUID laboratorioId);
}
