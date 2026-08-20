package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PeriodoLectivoServiceImpl implements PeriodoLectivoService {

    private final PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort;

    public PeriodoLectivoServiceImpl(PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort) {
        this.periodoLectivoRepositoryPort = periodoLectivoRepositoryPort;
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse crear(PeriodoLectivoRequest request) {

        validarCodigoDuplicado(request.codigo(), null);

        PeriodoLectivo periodo = PeriodoLectivo.nuevo(
                request.codigo(),
                request.nombre(),
                request.fechaInicio(),
                request.fechaFin(),
                request.estado()
        );

        PeriodoLectivo guardado = periodoLectivoRepositoryPort.guardar(periodo);

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PeriodoLectivoResponse> listar(String codigo, Pageable pageable) {
        return periodoLectivoRepositoryPort.buscar(codigo, pageable)
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
        return periodoLectivoRepositoryPort
                .buscarActualPorEstado(EstadoPeriodo.ACTIVO)
                .map(this::convertirAResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un periodo lectivo activo actualmente"));
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse actualizar(UUID id, PeriodoLectivoRequest request) {

        PeriodoLectivo periodo = buscarPeriodo(id);

        validarCodigoDuplicado(request.codigo(), id);

        periodo.actualizarDatos(
                request.codigo(),
                request.nombre(),
                request.fechaInicio(),
                request.fechaFin()
        );
        periodo.cambiarEstado(request.estado());

        PeriodoLectivo actualizado = periodoLectivoRepositoryPort.guardar(periodo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        return new ExisteResponse(id, periodoLectivoRepositoryPort.existePorId(id));
    }

    private PeriodoLectivo buscarPeriodo(UUID id) {
        return periodoLectivoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un periodo lectivo con el id: " + id));
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? periodoLectivoRepositoryPort.existeCodigo(codigo)
                : periodoLectivoRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un periodo lectivo con el código: " + codigo);
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
