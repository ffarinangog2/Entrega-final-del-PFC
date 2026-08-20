package ec.edu.scli.academico.application.service;

import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;

import java.util.List;
import java.util.UUID;

public interface HorarioAcademicoService {

    HorarioAcademicoResponse crear(HorarioAcademicoRequest request);

    List<HorarioAcademicoResponse> listar();

    HorarioAcademicoResponse obtenerPorId(UUID id);

    List<HorarioAcademicoResponse> listarPorDocente(UUID docenteId);

    List<HorarioAcademicoResponse> listarPorLaboratorio(UUID laboratorioId);
}
