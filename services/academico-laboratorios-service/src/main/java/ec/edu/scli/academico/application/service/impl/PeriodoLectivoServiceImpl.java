package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class PeriodoLectivoServiceImpl implements PeriodoLectivoService {

    private final PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort;
    private final Clock clock;

    @Autowired
    public PeriodoLectivoServiceImpl(PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort) {
        this(periodoLectivoRepositoryPort, Clock.systemDefaultZone());
    }

    PeriodoLectivoServiceImpl(PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort, Clock clock) {
        this.periodoLectivoRepositoryPort = periodoLectivoRepositoryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse crear(PeriodoLectivoRequest request) {

        validarCodigoDuplicado(request.codigo(), null);
        validarUnidadYSolapamiento(request, null);

        PeriodoLectivo periodo = PeriodoLectivo.nuevo(
                request.codigo(),
                request.nombre(),
                request.fechaInicio(),
                request.fechaFin(),
                request.estado()
        );
        periodo.definirUnidadAcademica(request.ppaCodigo(), request.ppaNombre(), request.cicloAcademico());

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
                .buscarVigente(LocalDate.now(clock))
                .map(this::convertirAResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un periodo lectivo vigente para la fecha del servidor"));
    }

    @Override
    @Transactional
    public PeriodoLectivoResponse actualizar(UUID id, PeriodoLectivoRequest request) {

        PeriodoLectivo periodo = buscarPeriodo(id);

        validarCodigoDuplicado(request.codigo(), id);
        validarUnidadYSolapamiento(request, id);

        periodo.actualizarDatos(
                request.codigo(),
                request.nombre(),
                request.fechaInicio(),
                request.fechaFin()
        );
        periodo.cambiarEstado(request.estado());
        periodo.definirUnidadAcademica(request.ppaCodigo(), request.ppaNombre(), request.cicloAcademico());

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

    private void validarUnidadYSolapamiento(PeriodoLectivoRequest request, UUID idActual) {
        if (request.cicloAcademico() == null) {
            return; // compatibilidad de registros históricos
        }
        var existentes = periodoLectivoRepositoryPort.buscar(null, Pageable.unpaged()).getContent();
        boolean duplicado = existentes.stream().filter(p -> !p.getId().equals(idActual)).anyMatch(p ->
                Objects.equals(p.getCicloAcademico(), request.cicloAcademico())
                        && anio(p.getPpaNombre()).equals(anio(request.ppaNombre())));
        if (duplicado) {
            throw new ConflictException("Ya existe el tipo académico para ese año");
        }
        boolean solapa = existentes.stream().filter(p -> !p.getId().equals(idActual)).anyMatch(p ->
                !request.fechaFin().isBefore(p.getFechaInicio()) && !request.fechaInicio().isAfter(p.getFechaFin()));
        if (solapa) {
            throw new ConflictException("Las fechas se solapan con otro período académico");
        }
    }

    private String anio(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll(".*?(\\d{4}-\\d{4}).*", "$1");
    }

    private PeriodoLectivoResponse convertirAResponse(PeriodoLectivo periodo) {
        return new PeriodoLectivoResponse(
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getNombre(),
                periodo.getFechaInicio(),
                periodo.getFechaFin(),
                periodo.getEstado(),
                periodo.getPpaCodigo(),
                periodo.getPpaNombre(),
                periodo.getCicloAcademico(),
                periodo.getCreadoEn(),
                periodo.getActualizadoEn()
        );
    }
}
