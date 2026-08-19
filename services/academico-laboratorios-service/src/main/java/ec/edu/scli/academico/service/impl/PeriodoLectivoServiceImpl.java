package ec.edu.scli.academico.service.impl;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoResponse;
import ec.edu.scli.academico.entity.PeriodoLectivo;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.exception.BusinessRuleException;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.PeriodoLectivoRepository;
import ec.edu.scli.academico.service.PeriodoLectivoService;
import ec.edu.scli.academico.specification.PeriodoLectivoSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PeriodoLectivoServiceImpl implements PeriodoLectivoService {

    private final PeriodoLectivoRepository periodoLectivoRepository;

    public PeriodoLectivoServiceImpl(PeriodoLectivoRepository periodoLectivoRepository) {
        this.periodoLectivoRepository = periodoLectivoRepository;
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse crear(PeriodoLectivoRequest request) {

        validarCodigoDuplicado(request.codigo(), null);
        validarFechas(request);

        PeriodoLectivo periodo = new PeriodoLectivo();
        periodo.setCodigo(request.codigo());
        periodo.setNombre(request.nombre());
        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());
        periodo.setEstado(request.estado() != null ? request.estado() : EstadoPeriodo.PLANIFICADO);

        PeriodoLectivo guardado = periodoLectivoRepository.save(periodo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PeriodoLectivoResponse> listar(String codigo, Pageable pageable) {

        Specification<PeriodoLectivo> specification =
                PeriodoLectivoSpecification.codigoContiene(codigo);

        return periodoLectivoRepository.findAll(specification, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodoLectivoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarPeriodo(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodoLectivoResponse obtenerActual() {
        return periodoLectivoRepository
                .findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo.ACTIVO)
                .map(this::convertirAResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un periodo lectivo activo actualmente"));
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse actualizar(UUID id, PeriodoLectivoRequest request) {

        PeriodoLectivo periodo = buscarPeriodo(id);

        validarCodigoDuplicado(request.codigo(), id);
        validarFechas(request);

        periodo.setCodigo(request.codigo());
        periodo.setNombre(request.nombre());
        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());

        if (request.estado() != null) {
            periodo.setEstado(request.estado());
        }

        PeriodoLectivo actualizado = periodoLectivoRepository.save(periodo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        boolean existe = periodoLectivoRepository.existsById(id);
        return new ExisteResponse(id, existe);
    }

    private PeriodoLectivo buscarPeriodo(UUID id) {
        return periodoLectivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un periodo lectivo con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? periodoLectivoRepository.existsByCodigo(codigo)
                : periodoLectivoRepository.existsByCodigoAndIdNot(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un periodo lectivo con el código: " + codigo);
        }
    }

    private void validarFechas(PeriodoLectivoRequest request) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new BusinessRuleException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private PeriodoLectivoResponse convertirAResponse(PeriodoLectivo periodo) {
        return new PeriodoLectivoResponse(
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getNombre(),
                periodo.getFechaInicio(),
                periodo.getFechaFin(),
                periodo.getEstado(),
                periodo.getCreadoEn(),
                periodo.getActualizadoEn()
        );
    }
}
