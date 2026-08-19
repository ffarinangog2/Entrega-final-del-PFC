package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.dto.horario.HorarioAcademicoResponse;
import ec.edu.scli.academico.entity.HorarioAcademico;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.HorarioAcademicoRepository;
import ec.edu.scli.academico.repository.LaboratorioRepository;
import ec.edu.scli.academico.repository.MateriaRepository;
import ec.edu.scli.academico.repository.PeriodoLectivoRepository;
import ec.edu.scli.academico.service.HorarioAcademicoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HorarioAcademicoServiceImpl implements HorarioAcademicoService {

    private final HorarioAcademicoRepository horarioAcademicoRepository;
    private final MateriaRepository materiaRepository;
    private final PeriodoLectivoRepository periodoLectivoRepository;
    private final LaboratorioRepository laboratorioRepository;

    public HorarioAcademicoServiceImpl(
            HorarioAcademicoRepository horarioAcademicoRepository,
            MateriaRepository materiaRepository,
            PeriodoLectivoRepository periodoLectivoRepository,
            LaboratorioRepository laboratorioRepository
    ) {
        this.horarioAcademicoRepository = horarioAcademicoRepository;
        this.materiaRepository = materiaRepository;
        this.periodoLectivoRepository = periodoLectivoRepository;
        this.laboratorioRepository = laboratorioRepository;
    }

    @Override
    @Transactional
    public HorarioAcademicoResponse crear(HorarioAcademicoRequest request) {

        validarMateriaExiste(request.materiaId());
        validarPeriodoLectivoExiste(request.periodoLectivoId());
        validarLaboratorioExisteSiSePropociona(request.laboratorioId());
        validarHoras(request);

        HorarioAcademico horario = new HorarioAcademico();
        horario.setMateriaId(request.materiaId());
        horario.setPeriodoLectivoId(request.periodoLectivoId());
        horario.setLaboratorioId(request.laboratorioId());
        // docente_id es un UUID externo de usuarios-service: no se valida aquí,
        // solo se almacena tal como llega desde el cliente/gateway
        horario.setDocenteId(request.docenteId());
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setParalelo(request.paralelo());
        horario.setActivo(true);

        HorarioAcademico guardado = horarioAcademicoRepository.save(horario);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioAcademicoResponse> listar() {
        return horarioAcademicoRepository.findAll()
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
        return horarioAcademicoRepository.findByDocenteId(docenteId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioAcademicoResponse> listarPorLaboratorio(UUID laboratorioId) {
        return horarioAcademicoRepository.findByLaboratorioId(laboratorioId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    private HorarioAcademico buscarHorario(UUID id) {
        return horarioAcademicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un horario académico con el id: " + id));
    }

    private void validarMateriaExiste(UUID materiaId) {
        if (!materiaRepository.existsById(materiaId)) {
            throw new BusinessRuleException("No existe una materia con el id: " + materiaId);
        }
    }

    private void validarPeriodoLectivoExiste(UUID periodoLectivoId) {
        if (!periodoLectivoRepository.existsById(periodoLectivoId)) {
            throw new BusinessRuleException(
                    "No existe un periodo lectivo con el id: " + periodoLectivoId);
        }
    }

    private void validarLaboratorioExisteSiSePropociona(UUID laboratorioId) {
        if (laboratorioId != null && !laboratorioRepository.existsById(laboratorioId)) {
            throw new BusinessRuleException(
                    "No existe un laboratorio con el id: " + laboratorioId);
        }
    }

    private void validarHoras(HorarioAcademicoRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new BusinessRuleException(
                    "La hora de fin debe ser posterior a la hora de inicio");
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
