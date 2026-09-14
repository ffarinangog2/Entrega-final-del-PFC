package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.infrastructure.audit.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
public class LaboratorioServiceImpl implements LaboratorioService {

    private final LaboratorioRepositoryPort laboratorioRepositoryPort;
    private final PisoRepositoryPort pisoRepositoryPort;
    private final AuditLogger auditLogger;

    public LaboratorioServiceImpl(
            LaboratorioRepositoryPort laboratorioRepositoryPort,
            PisoRepositoryPort pisoRepositoryPort,
            AuditLogger auditLogger
    ) {
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
        this.pisoRepositoryPort = pisoRepositoryPort;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public LaboratorioResponse crear(LaboratorioRequest request) {

        validarPisoExiste(request.pisoId());
        validarCodigoDuplicado(request.codigo(), null);

        Laboratorio laboratorio = Laboratorio.nuevo(
                request.pisoId(),
                request.codigo(),
                request.nombre(),
                request.capacidad(),
                request.descripcion()
        );

        Laboratorio guardado = laboratorioRepositoryPort.guardar(laboratorio);

        auditLogger.registrarEvento(
                "laboratorio_creado",
                usuarioActual(),
                ipCliente(),
                "id=" + guardado.getId());

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LaboratorioResponse> listar(String texto, EstadoLaboratorio estado, Boolean activo, Pageable pageable) {
        return laboratorioRepositoryPort.buscar(texto, estado, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratorioResponse> listarDisponibles() {
        return laboratorioRepositoryPort.buscarDisponibles()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarLaboratorio(id));
    }

    @Override
    @Transactional
    public LaboratorioResponse actualizar(UUID id, LaboratorioRequest request) {

        Laboratorio laboratorio = buscarLaboratorio(id);

        validarPisoExiste(request.pisoId());
        validarCodigoDuplicado(request.codigo(), id);

        laboratorio.actualizarDatos(
                request.pisoId(),
                request.codigo(),
                request.nombre(),
                request.capacidad(),
                request.descripcion()
        );

        Laboratorio actualizado = laboratorioRepositoryPort.guardar(laboratorio);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public LaboratorioResponse cambiarEstado(UUID id, EstadoLaboratorio estado) {

        Laboratorio laboratorio = buscarLaboratorio(id);
        EstadoLaboratorio estadoAnterior = laboratorio.getEstado();
        laboratorio.cambiarEstado(estado);

        Laboratorio actualizado = laboratorioRepositoryPort.guardar(laboratorio);

        auditLogger.registrarEvento(
                "laboratorio_estado_cambiado",
                usuarioActual(),
                ipCliente(),
                "id=" + id + ", estadoAnterior=" + estadoAnterior + ", estadoNuevo=" + estado);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratorioDisponibilidadBaseResponse obtenerDisponibilidadBase(UUID id) {

        return laboratorioRepositoryPort.buscarPorId(id)
                .map(laboratorio -> new LaboratorioDisponibilidadBaseResponse(
                        laboratorio.getId(),
                        laboratorio.getPisoId(),
                        true,
                        laboratorio.isActivo(),
                        laboratorio.getEstado(),
                        laboratorio.getCapacidad()
                ))
                .orElseGet(() -> new LaboratorioDisponibilidadBaseResponse(
                        id, null, false, false, null, null
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ExisteResponse verificarExistencia(UUID id) {
        return new ExisteResponse(id, laboratorioRepositoryPort.existePorId(id));
    }

    private Laboratorio buscarLaboratorio(UUID id) {
        return laboratorioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un laboratorio con el id: " + id));
    }

    private void validarPisoExiste(UUID pisoId) {
        if (pisoRepositoryPort.buscarPorId(pisoId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe un piso con el id: " + pisoId);
        }
    }

    private void validarCodigoDuplicado(String codigo, UUID idActual) {

        boolean existe = (idActual == null)
                ? laboratorioRepositoryPort.existeCodigo(codigo)
                : laboratorioRepositoryPort.existeCodigoParaOtroId(codigo, idActual);

        if (existe) {
            throw new ConflictException("Ya existe un laboratorio con el código: " + codigo);
        }
    }

    private LaboratorioResponse convertirAResponse(Laboratorio laboratorio) {
        return new LaboratorioResponse(
                laboratorio.getId(),
                laboratorio.getPisoId(),
                laboratorio.getCodigo(),
                laboratorio.getNombre(),
                laboratorio.getCapacidad(),
                laboratorio.getDescripcion(),
                laboratorio.getEstado(),
                laboratorio.isActivo(),
                laboratorio.getCreadoEn(),
                laboratorio.getActualizadoEn()
        );
    }

    private String usuarioActual() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {

            return "sistema";
        }

        return authentication.getName();
    }

    private String ipCliente() {

        var attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {

            return "desconocida";
        }

        HttpServletRequest request = servletAttributes.getRequest();

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {

            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
