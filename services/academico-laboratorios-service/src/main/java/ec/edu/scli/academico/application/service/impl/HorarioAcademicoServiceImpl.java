package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.HorarioAcademicoService;
import ec.edu.scli.academico.domain.model.HorarioAcademico;
import ec.edu.scli.academico.domain.port.HorarioAcademicoRepositoryPort;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.MateriaRepositoryPort;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HorarioAcademicoServiceImpl implements HorarioAcademicoService {

    private final HorarioAcademicoRepositoryPort horarioAcademicoRepositoryPort;
    private final MateriaRepositoryPort materiaRepositoryPort;
    private final PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort;
    private final LaboratorioRepositoryPort laboratorioRepositoryPort;

    public HorarioAcademicoServiceImpl(
            HorarioAcademicoRepositoryPort horarioAcademicoRepositoryPort,
            MateriaRepositoryPort materiaRepositoryPort,
            PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort,
            LaboratorioRepositoryPort laboratorioRepositoryPort
    ) {
        this.horarioAcademicoRepositoryPort = horarioAcademicoRepositoryPort;
        this.materiaRepositoryPort = materiaRepositoryPort;
        this.periodoLectivoRepositoryPort = periodoLectivoRepositoryPort;
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
    }

    @Override
    @Transactional
    public HorarioAcademicoResponse crear(HorarioAcademicoRequest request) {

        validarMateriaExiste(request.materiaId());
        validarPeriodoLectivoExiste(request.periodoLectivoId());
        validarLaboratorioExisteSiSeProporciona(request.laboratorioId());

        // docente_id es un UUID externo de usuarios-service: no se valida aquí,
        // solo se almacena tal como llega desde el cliente/gateway.
        HorarioAcademico horario = HorarioAcademico.nuevo(
                request.materiaId(),
                request.periodoLectivoId(),
                request.laboratorioId(),
                request.docenteId(),
                request.diaSemana(),
                request.horaInicio(),
                request.horaFin(),
                request.paralelo()
        );

        HorarioAcademico guardado = horarioAcademicoRepositoryPort.guardar(horario);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioAcademicoResponse> listar() {
        return horarioAcademicoRepositoryPort.buscarTodos()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HorarioAcademicoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarHorario(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioAcademicoResponse> listarPorDocente(UUID docenteId) {
        return horarioAcademicoRepositoryPort.buscarPorDocente(docenteId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioAcademicoResponse> listarPorLaboratorio(UUID laboratorioId) {
        return horarioAcademicoRepositoryPort.buscarPorLaboratorio(laboratorioId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private HorarioAcademico buscarHorario(UUID id) {
        return horarioAcademicoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un horario académico con el id: " + id));
    }

    private void validarMateriaExiste(UUID materiaId) {
        if (!materiaRepositoryPort.existePorId(materiaId)) {
            throw new BusinessRuleException("No existe una materia con el id: " + materiaId);
        }
    }

    private void validarPeriodoLectivoExiste(UUID periodoLectivoId) {
        if (!periodoLectivoRepositoryPort.existePorId(periodoLectivoId)) {
            throw new BusinessRuleException(
                    "No existe un periodo lectivo con el id: " + periodoLectivoId);
        }
    }

    private void validarLaboratorioExisteSiSeProporciona(UUID laboratorioId) {
        if (laboratorioId != null && !laboratorioRepositoryPort.existePorId(laboratorioId)) {
            throw new BusinessRuleException(
                    "No existe un laboratorio con el id: " + laboratorioId);
        }
    }

    private HorarioAcademicoResponse convertirAResponse(HorarioAcademico horario) {
        return new HorarioAcademicoResponse(
                horario.getId(),
                horario.getMateriaId(),
                horario.getPeriodoLectivoId(),
                horario.getLaboratorioId(),
                horario.getDocenteId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getParalelo(),
                horario.isActivo(),
                horario.getCreadoEn(),
                horario.getActualizadoEn()
        );
    }
}
