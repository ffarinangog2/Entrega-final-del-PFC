package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.domain.port.EquipoRepositoryPort;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
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
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepositoryPort equipoRepositoryPort;
    private final LaboratorioRepositoryPort laboratorioRepositoryPort;
    private final TipoEquipoRepositoryPort tipoEquipoRepositoryPort;
    private final AuditLogger auditLogger;

    public EquipoServiceImpl(
            EquipoRepositoryPort equipoRepositoryPort,
            LaboratorioRepositoryPort laboratorioRepositoryPort,
            TipoEquipoRepositoryPort tipoEquipoRepositoryPort,
            AuditLogger auditLogger
    ) {
        this.equipoRepositoryPort = equipoRepositoryPort;
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
        this.tipoEquipoRepositoryPort = tipoEquipoRepositoryPort;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public EquipoResponse crear(EquipoRequest request) {

        validarLaboratorioExiste(request.laboratorioId());
        validarTipoEquipoExiste(request.tipoEquipoId());
        validarCodigoInventarioDuplicado(request.codigoInventario(), null);
        validarNumeroSerieDuplicado(request.numeroSerie(), null);

        Equipo equipo = Equipo.nuevo(
                request.laboratorioId(),
                request.tipoEquipoId(),
                request.codigoInventario(),
                request.numeroSerie(),
                request.marca(),
                request.modelo(),
                request.procesador(),
                request.memoriaRam(),
                request.almacenamiento(),
                request.direccionIp(),
                request.direccionMac(),
                request.observacion()
        );

        Equipo guardado = equipoRepositoryPort.guardar(equipo);

        auditLogger.registrarEvento(
                "equipo_creado",
                usuarioActual(),
                ipCliente(),
                "id=" + guardado.getId());

        return convertirAResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EquipoResponse> listar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable) {
        return equipoRepositoryPort.buscar(laboratorioId, estado, activo, pageable)
                .map(this::convertirAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPorLaboratorio(UUID laboratorioId) {

        validarLaboratorioExiste(laboratorioId);

        return equipoRepositoryPort.buscarPorLaboratorio(laboratorioId)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EquipoResponse obtenerPorId(UUID id) {
        return convertirAResponse(buscarEquipo(id));
    }

    @Override
    @Transactional
    public EquipoResponse actualizar(UUID id, EquipoRequest request) {

        Equipo equipo = buscarEquipo(id);

        validarLaboratorioExiste(request.laboratorioId());
        validarTipoEquipoExiste(request.tipoEquipoId());
        validarCodigoInventarioDuplicado(request.codigoInventario(), id);
        validarNumeroSerieDuplicado(request.numeroSerie(), id);

        equipo.aplicarDatos(
                request.laboratorioId(),
                request.tipoEquipoId(),
                request.codigoInventario(),
                request.numeroSerie(),
                request.marca(),
                request.modelo(),
                request.procesador(),
                request.memoriaRam(),
                request.almacenamiento(),
                request.direccionIp(),
                request.direccionMac(),
                request.observacion()
        );

        Equipo actualizado = equipoRepositoryPort.guardar(equipo);

        return convertirAResponse(actualizado);
    }

    @Override
    @Transactional
    public EquipoResponse cambiarEstado(UUID id, EstadoEquipo estado) {

        Equipo equipo = buscarEquipo(id);
        EstadoEquipo estadoAnterior = equipo.getEstado();
        equipo.cambiarEstado(estado);

        Equipo actualizado = equipoRepositoryPort.guardar(equipo);

        auditLogger.registrarEvento(
                "equipo_estado_cambiado",
                usuarioActual(),
                ipCliente(),
                "id=" + id + ", estadoAnterior=" + estadoAnterior + ", estadoNuevo=" + estado);

        return convertirAResponse(actualizado);
    }

    private Equipo buscarEquipo(UUID id) {
        return equipoRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un equipo con el id: " + id));
    }

    private void validarLaboratorioExiste(UUID laboratorioId) {
        if (laboratorioRepositoryPort.buscarPorId(laboratorioId).isEmpty()) {
            throw new BusinessRuleException(
                    "No existe un laboratorio con el id: " + laboratorioId);
        }
    }

    private void validarTipoEquipoExiste(UUID tipoEquipoId) {
        if (!tipoEquipoRepositoryPort.existePorId(tipoEquipoId)) {
            throw new BusinessRuleException(
                    "No existe un tipo de equipo con el id: " + tipoEquipoId);
        }
    }

    private void validarCodigoInventarioDuplicado(String codigoInventario, UUID idActual) {

        boolean existe = (idActual == null)
                ? equipoRepositoryPort.existeCodigoInventario(codigoInventario)
                : equipoRepositoryPort.existeCodigoInventarioParaOtroId(codigoInventario, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un equipo con el código de inventario: " + codigoInventario);
        }
    }

    private void validarNumeroSerieDuplicado(String numeroSerie, UUID idActual) {

        if (numeroSerie == null || numeroSerie.isBlank()) {
            return;
        }

        boolean existe = (idActual == null)
                ? equipoRepositoryPort.existeNumeroSerie(numeroSerie)
                : equipoRepositoryPort.existeNumeroSerieParaOtroId(numeroSerie, idActual);

        if (existe) {
            throw new ConflictException(
                    "Ya existe un equipo con el número de serie: " + numeroSerie);
        }
    }

    private EquipoResponse convertirAResponse(Equipo equipo) {
        return new EquipoResponse(
                equipo.getId(),
                equipo.getLaboratorioId(),
                equipo.getTipoEquipoId(),
                equipo.getCodigoInventario(),
                equipo.getNumeroSerie(),
                equipo.getMarca(),
                equipo.getModelo(),
                equipo.getProcesador(),
                equipo.getMemoriaRam(),
                equipo.getAlmacenamiento(),
                equipo.getDireccionIp(),
                equipo.getDireccionMac(),
                equipo.getEstado(),
                equipo.getObservacion(),
                equipo.isActivo(),
                equipo.getCreadoEn(),
                equipo.getActualizadoEn()
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
