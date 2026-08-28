package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.ExisteExternoResponse;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.presentation.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerAlternativaRequest;
import ec.edu.scli.reservas.presentation.dto.request.ResponderPropuestaRequest;
import ec.edu.scli.reservas.presentation.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.presentation.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import ec.edu.scli.reservas.domain.model.HistorialSolicitud;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.domain.model.FiltroSolicitudReserva;
import ec.edu.scli.reservas.domain.model.Pagina;
import ec.edu.scli.reservas.mapper.HistorialSolicitudMapper;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.mapper.SolicitudReservaMapper;
import ec.edu.scli.reservas.observability.BusinessEventMetrics;
import ec.edu.scli.reservas.domain.port.out.HistorialSolicitudRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaAprobacionRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaCreacionSolicitudRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.domain.state.reserva.ReservaStates;
import ec.edu.scli.reservas.domain.state.solicitud.SolicitudReservaStates;
import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.domain.port.out.AgendaMutexPort;
import ec.edu.scli.reservas.domain.port.out.DocenteInstitucionalPort;
import ec.edu.scli.reservas.infrastructure.audit.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Implementa las operaciones de negocio de las solicitudes de reserva. */
@Service
public class SolicitudReservaServiceImpl implements SolicitudReservaService {

    private final SolicitudReservaRepositoryPort solicitudReservaRepository;
    private final ReservaRepositoryPort reservaRepository;
    private final HistorialSolicitudRepositoryPort historialSolicitudRepository;
    private final IdempotenciaAprobacionRepositoryPort idempotenciaAprobacionRepository;
    private final IdempotenciaCreacionSolicitudRepositoryPort idempotenciaCreacionRepository;
    private final SolicitudReservaMapper solicitudReservaMapper;
    private final ReservaMapper reservaMapper;
    private final HistorialSolicitudMapper historialSolicitudMapper;
    private final DocenteInstitucionalPort docentes;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final DisponibilidadService disponibilidadService;
    private final BusinessEventMetrics businessEventMetrics;
    private final PoliticaAmbitoLaboratorio politicaAmbito;
    private final AgendaMutexPort agendaMutex;
    private final AuditLogger auditLogger;

    public SolicitudReservaServiceImpl(
            SolicitudReservaRepositoryPort solicitudReservaRepository,
            ReservaRepositoryPort reservaRepository,
            HistorialSolicitudRepositoryPort historialSolicitudRepository,
            IdempotenciaAprobacionRepositoryPort idempotenciaAprobacionRepository,
            IdempotenciaCreacionSolicitudRepositoryPort idempotenciaCreacionRepository,
            SolicitudReservaMapper solicitudReservaMapper,
            ReservaMapper reservaMapper,
            HistorialSolicitudMapper historialSolicitudMapper,
            DocenteInstitucionalPort docentes,
            AcademicoLaboratoriosClient academicoLaboratoriosClient,
            DisponibilidadService disponibilidadService,
            BusinessEventMetrics businessEventMetrics,
            PoliticaAmbitoLaboratorio politicaAmbito,
            AgendaMutexPort agendaMutex,
            AuditLogger auditLogger) {
        this.solicitudReservaRepository = solicitudReservaRepository;
        this.reservaRepository = reservaRepository;
        this.historialSolicitudRepository = historialSolicitudRepository;
        this.idempotenciaAprobacionRepository = idempotenciaAprobacionRepository;
        this.idempotenciaCreacionRepository = idempotenciaCreacionRepository;
        this.solicitudReservaMapper = solicitudReservaMapper;
        this.reservaMapper = reservaMapper;
        this.historialSolicitudMapper = historialSolicitudMapper;
        this.docentes = docentes;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
        this.disponibilidadService = disponibilidadService;
        this.businessEventMetrics = businessEventMetrics;
        this.politicaAmbito = politicaAmbito;
        this.agendaMutex = agendaMutex;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 100))
    public SolicitudReservaResponse crear(
            CrearSolicitudReservaRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        String payloadHash = hashCreacion(request);
        idempotenciaCreacionRepository.registrarSiAusente(
                claveIdempotencia, usuarioAutenticadoId, payloadHash);
        var operacion = idempotenciaCreacionRepository.buscarParaActualizar(claveIdempotencia)
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible registrar la creación idempotente"));
        if (!"CREAR_SOLICITUD".equals(operacion.operacion())
                || !usuarioAutenticadoId.equals(operacion.actorId())
                || !payloadHash.equals(operacion.payloadHash())) {
            throw new IllegalStateException(
                    "La clave de idempotencia ya fue utilizada con otro actor o payload");
        }
        if (operacion.solicitudId() != null) {
            return solicitudReservaMapper.toResponse(solicitudReservaRepository
                    .buscarPorId(operacion.solicitudId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El resultado de la creación idempotente no existe")));
        }
        SolicitudReservaResponse respuesta = crearNuevaSolicitud(
                request, claveIdempotencia, usuarioAutenticadoId);
        idempotenciaCreacionRepository.completar(claveIdempotencia, respuesta.id());
        return respuesta;
    }

    String hashCreacion(CrearSolicitudReservaRequest request) {
        String canonical = String.join("\u001f",
                value(request.solicitanteId()), value(request.docenteId()),
                value(request.laboratorioId()), value(request.materiaId()),
                value(request.periodoLectivoId()), value(request.fechaReserva()),
                value(request.horaInicio()), value(request.horaFin()),
                value(request.numeroParticipantes()), value(request.motivo()),
                value(request.observacion()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private String value(Object value) {
        if (value == null) {
            return "-1:";
        }
        String text = value.toString();
        return text.length() + ":" + text;
    }

    private SolicitudReservaResponse crearNuevaSolicitud(
            CrearSolicitudReservaRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        UUID docenteId = resolverDocente(request.docenteId(), usuarioAutenticadoId);
        validarLaboratorio(request.laboratorioId());
        validarMateria(request.materiaId());
        validarPeriodoLectivo(request.periodoLectivoId());
        validarDisponibilidad(
                request.laboratorioId(), request.fechaReserva(), request.horaInicio(), request.horaFin());

        SolicitudReserva solicitud = BeanUtils.instantiateClass(SolicitudReserva.class);
        solicitud.setSolicitanteId(request.solicitanteId());
        solicitud.setDocenteId(docenteId);
        solicitud.setLaboratorioId(request.laboratorioId());
        solicitud.setPisoId(politicaAmbito.obtenerPiso(request.laboratorioId()));
        solicitud.setMateriaId(request.materiaId());
        solicitud.setPeriodoLectivoId(request.periodoLectivoId());
        solicitud.setFechaReserva(request.fechaReserva());
        solicitud.setHoraInicio(request.horaInicio());
        solicitud.setHoraFin(request.horaFin());
        solicitud.setNumeroParticipantes(request.numeroParticipantes());
        solicitud.setMotivo(request.motivo());
        solicitud.setObservacion(request.observacion());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setClaveIdempotencia(claveIdempotencia);

        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(guardada.getId());
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(EstadoSolicitud.PENDIENTE);
        historial.setComentario("Solicitud creada");
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudCreada();

        auditLogger.registrarEvento(
                "solicitud_creada",
                usuarioActual(),
                ipCliente(),
                "id=" + guardada.getId());

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudReservaResponse> listar(
            EstadoSolicitud estado,
            UUID solicitanteId,
            UUID laboratorioId,
            LocalDate fecha,
            int pagina,
            int tamanio) {
        return mapearPagina(solicitudReservaRepository.buscar(
                new FiltroSolicitudReserva(estado, solicitanteId, laboratorioId, null, fecha), pagina, tamanio));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudReservaResponse> listarAutorizado(
            EstadoSolicitud estado, UUID solicitanteId, UUID laboratorioId, LocalDate fecha,
            int pagina, int tamanio, UUID actorId) {
        var actor = politicaAmbito.actor();
        if (!actor.perfilId().equals(actorId)) throw new AccessDeniedException("Actor inconsistente");
        UUID pisoId = null;
        if (actor.tiene("ROLE_DOCENTE")) solicitanteId = actorId;
        else if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) pisoId = politicaAmbito.pisoGestionado();
        else if (!actor.tiene("ROLE_ADMINISTRADOR")) {
            solicitanteId = actorId;
        }
        return mapearPagina(solicitudReservaRepository.buscar(
                new FiltroSolicitudReserva(estado, solicitanteId, laboratorioId, pisoId, fecha), pagina, tamanio));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudReservaResponse buscarPorId(UUID id) {
        return solicitudReservaMapper.toResponse(obtenerSolicitud(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudReservaResponse buscarPorIdAutorizado(UUID id, UUID actorId) {
        SolicitudReserva solicitud = obtenerSolicitud(id);
        validarLectura(solicitud, actorId);
        return solicitudReservaMapper.toResponse(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudReservaResponse> listarPorSolicitante(
            UUID solicitanteId, int pagina, int tamanio) {
        return mapearPagina(solicitudReservaRepository.buscarPorSolicitante(
                solicitanteId, pagina, tamanio));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudReservaResponse> listarPorEstado(
            EstadoSolicitud estado, int pagina, int tamanio) {
        return mapearPagina(solicitudReservaRepository.buscarPorEstado(
                estado, pagina, tamanio));
    }

    @Override
    @Transactional
    public SolicitudReservaResponse actualizar(
            UUID id,
            ActualizarSolicitudReservaRequest request,
            UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = obtenerSolicitud(id);
        validarPropietario(solicitud, usuarioAutenticadoId);
        SolicitudReservaStates.desde(solicitud.getEstado()).validarActualizacion();

        UUID docenteId = resolverDocente(request.docenteId(), usuarioAutenticadoId);
        validarLaboratorio(request.laboratorioId());
        validarMateria(request.materiaId());
        validarPeriodoLectivo(request.periodoLectivoId());
        validarDisponibilidad(
                request.laboratorioId(), request.fechaReserva(), request.horaInicio(), request.horaFin());

        solicitud.setDocenteId(docenteId);
        solicitud.setLaboratorioId(request.laboratorioId());
        solicitud.setPisoId(politicaAmbito.obtenerPiso(request.laboratorioId()));
        solicitud.setMateriaId(request.materiaId());
        solicitud.setPeriodoLectivoId(request.periodoLectivoId());
        solicitud.setFechaReserva(request.fechaReserva());
        solicitud.setHoraInicio(request.horaInicio());
        solicitud.setHoraFin(request.horaFin());
        solicitud.setNumeroParticipantes(request.numeroParticipantes());
        solicitud.setMotivo(request.motivo());
        solicitud.setObservacion(request.observacion());

        return solicitudReservaMapper.toResponse(solicitudReservaRepository.guardar(solicitud));
    }

    private void validarPropietario(SolicitudReserva solicitud, UUID usuarioAutenticadoId) {
        if (!usuarioAutenticadoId.equals(solicitud.getSolicitanteId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La solicitud pertenece a otro usuario");
        }
    }

    private SolicitudReserva obtenerSolicitud(UUID id) {
        return solicitudReservaRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
    }

    private UUID resolverDocente(UUID docenteIdSolicitado, UUID actorId) {
        var actor = politicaAmbito.actor();
        if (!actor.perfilId().equals(actorId)) throw new AccessDeniedException("Actor inconsistente");
        var docente = actor.tiene("ROLE_DOCENTE")
                ? docentes.obtenerPorPerfilId(actorId)
                : docentes.obtenerPorDocenteId(docenteIdSolicitado);
        if (docente == null || docente.docenteId() == null) {
            throw new ResourceNotFoundException("No existe un docente asociado al perfil indicado");
        }
        if (!docente.activo()) {
            throw new IllegalArgumentException("El docente indicado no está activo");
        }
        if (actor.tiene("ROLE_DOCENTE") && !docente.docenteId().equals(docenteIdSolicitado)) {
            throw new AccessDeniedException("Un docente no puede suplantar a otro docente");
        }
        return docente.docenteId();
    }

    private void validarLectura(SolicitudReserva solicitud, UUID actorId) {
        var actor = politicaAmbito.actor();
        if (!actor.perfilId().equals(actorId)) throw new AccessDeniedException("Actor inconsistente");
        if (solicitud.getSolicitanteId().equals(actorId)) return;
        if (actor.tiene("ROLE_ADMINISTRADOR")) return;
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) {
            politicaAmbito.validarGestion(solicitud.getLaboratorioId());
            return;
        }
        throw new AccessDeniedException("La solicitud pertenece a otro usuario");
    }

    private void validarGestion(SolicitudReserva solicitud, UUID actorId) {
        if (!politicaAmbito.actor().perfilId().equals(actorId)) {
            throw new AccessDeniedException("Actor inconsistente");
        }
        politicaAmbito.validarGestion(solicitud.getLaboratorioId());
    }

    private void validarLaboratorio(UUID laboratorioId) {
        LaboratorioExternoResponse laboratorio =
                academicoLaboratoriosClient.obtenerLaboratorio(laboratorioId);
        if (laboratorio == null || !laboratorio.existe()) {
            throw new ResourceNotFoundException("El laboratorio indicado no existe");
        }
        if (!laboratorio.activo()) {
            throw new IllegalArgumentException("El laboratorio indicado no está activo");
        }
    }

    private void validarMateria(UUID materiaId) {
        ExisteExternoResponse materia = academicoLaboratoriosClient.verificarMateria(materiaId);
        if (materia == null || !materia.existe()) {
            throw new ResourceNotFoundException("La materia indicada no existe");
        }
    }

    private void validarPeriodoLectivo(UUID periodoLectivoId) {
        ExisteExternoResponse periodo =
                academicoLaboratoriosClient.verificarPeriodoLectivo(periodoLectivoId);
        if (periodo == null || !periodo.existe()) {
            throw new ResourceNotFoundException("El período lectivo indicado no existe");
        }
    }

    private void validarDisponibilidad(
            UUID laboratorioId,
            LocalDate fecha,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin) {
        DisponibilidadResponse disponibilidad =
                disponibilidadService.consultar(laboratorioId, fecha, horaInicio, horaFin);
        if (disponibilidad == null) {
            throw new IllegalStateException("No fue posible determinar la disponibilidad");
        }
        if (!disponibilidad.disponible()) {
            throw new IllegalStateException(disponibilidad.motivo());
        }
    }

    @Override
    @Transactional
    public SolicitudReservaResponse ponerEnRevision(UUID id, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));

        validarGestion(solicitud, usuarioAutenticadoId);
        solicitud.setEstado(SolicitudReservaStates.desde(solicitud.getEstado()).ponerEnRevision());
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(guardada.getId());
        historial.setEstadoAnterior(EstadoSolicitud.PENDIENTE);
        historial.setEstadoNuevo(EstadoSolicitud.EN_REVISION);
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historial.setComentario("Solicitud puesta en revisión");
        historialSolicitudRepository.guardar(historial);

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 100))
    public ReservaResponse aprobar(
            UUID id,
            AprobarSolicitudRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        idempotenciaAprobacionRepository.registrarSiAusente(claveIdempotencia, id);
        var operacionIdempotente = idempotenciaAprobacionRepository
                .buscarParaActualizar(claveIdempotencia)
                .orElseThrow(() -> new IllegalStateException(
                        "No fue posible registrar la operación idempotente de aprobación"));

        if (!id.equals(operacionIdempotente.solicitudId())) {
            throw new IllegalStateException(
                    "La clave de idempotencia ya fue utilizada para otra solicitud");
        }
        if (!"APROBAR_SOLICITUD".equals(operacionIdempotente.operacion())) {
            throw new IllegalStateException(
                    "La clave de idempotencia pertenece a otra operación");
        }
        if (operacionIdempotente.reservaId() != null) {
            Reserva resultado = reservaRepository
                    .buscarPorId(operacionIdempotente.reservaId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El resultado de la aprobación idempotente no existe"));
            politicaAmbito.validarGestion(resultado.getLaboratorioId());
            return reservaMapper.toResponse(resultado);
        }

        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));

        validarGestion(solicitud, usuarioAutenticadoId);
        EstadoSolicitud estadoAprobado = SolicitudReservaStates.desde(solicitud.getEstado()).aprobar();

        if (reservaRepository.existePorSolicitudId(id)) {
            throw new IllegalStateException(
                    "La solicitud ya tiene una reserva asociada");
        }

        agendaMutex.bloquear(solicitud.getLaboratorioId(), solicitud.getFechaReserva());
        validarDisponibilidad(
                solicitud.getLaboratorioId(),
                solicitud.getFechaReserva(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin());
        validarLaboratorio(solicitud.getLaboratorioId());

        Reserva reserva = BeanUtils.instantiateClass(Reserva.class);
        reserva.setSolicitudId(solicitud.getId());
        reserva.setLaboratorioId(solicitud.getLaboratorioId());
        reserva.setPisoId(solicitud.getPisoId());
        reserva.setResponsableId(usuarioAutenticadoId);
        reserva.setFechaReserva(solicitud.getFechaReserva());
        reserva.setHoraInicio(solicitud.getHoraInicio());
        reserva.setHoraFin(solicitud.getHoraFin());
        reserva.setEstado(EstadoReserva.PROGRAMADA);
        reserva.setCodigoReserva(generarCodigoReserva(solicitud.getFechaReserva()));

        Reserva guardada = reservaRepository.guardar(reserva);

        solicitud.setEstado(estadoAprobado);
        solicitud.setReservaId(guardada.getId());
        solicitudReservaRepository.guardar(solicitud);

        idempotenciaAprobacionRepository.completar(claveIdempotencia, guardada.getId());

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(solicitud.getId());
        historial.setEstadoAnterior(EstadoSolicitud.EN_REVISION);
        historial.setEstadoNuevo(EstadoSolicitud.APROBADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudAprobada();
        businessEventMetrics.reservaCreada();

        auditLogger.registrarEvento(
                "solicitud_aprobada",
                usuarioActual(),
                ipCliente(),
                "id=" + solicitud.getId());

        return reservaMapper.toResponse(guardada);
    }

    private String generarCodigoReserva(LocalDate fechaReserva) {
        String identificador = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "RES-" + fechaReserva.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + identificador;
    }

    @Override
    @Transactional
    public SolicitudReservaResponse rechazar(
            UUID id, RechazarSolicitudRequest request, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
        validarGestion(solicitud, usuarioAutenticadoId);
        solicitud.setEstado(SolicitudReservaStates.desde(solicitud.getEstado()).rechazar());
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(guardada.getId());
        historial.setEstadoAnterior(EstadoSolicitud.EN_REVISION);
        historial.setEstadoNuevo(EstadoSolicitud.RECHAZADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudRechazada();

        auditLogger.registrarEvento(
                "solicitud_rechazada",
                usuarioActual(),
                ipCliente(),
                "id=" + guardada.getId());

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 100))
    public SolicitudReservaResponse cancelar(
            UUID id, CancelarSolicitudRequest request, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
        boolean propietario = solicitud.getSolicitanteId().equals(usuarioAutenticadoId);
        if (!propietario) validarGestion(solicitud, usuarioAutenticadoId);
        EstadoSolicitud anterior = solicitud.getEstado();
        EstadoSolicitud estadoCancelado = SolicitudReservaStates.desde(anterior).cancelar();

        if (anterior == EstadoSolicitud.APROBADA) {
            Reserva reservaAsociada = reservaRepository.buscarPorSolicitudId(id)
                    .orElseThrow(() -> new IllegalStateException(
                            "La solicitud aprobada no tiene una reserva asociada"));
            Reserva reserva = reservaRepository.buscarPorIdParaActualizar(reservaAsociada.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "La solicitud aprobada no tiene una reserva asociada"));
            reserva.setEstado(ReservaStates.desde(reserva.getEstado()).cancelar());
            reservaRepository.guardar(reserva);
            businessEventMetrics.reservaCancelada();
        }

        solicitud.setEstado(estadoCancelado);
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(guardada.getId());
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(EstadoSolicitud.CANCELADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudCancelada();

        auditLogger.registrarEvento(
                "solicitud_cancelada",
                usuarioActual(),
                ipCliente(),
                "id=" + guardada.getId());

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitudReservaResponse proponerAlternativa(
            UUID id, ProponerAlternativaRequest request, UUID actorId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
        validarGestion(solicitud, actorId);
        politicaAmbito.validarGestion(request.laboratorioId());
        validarLaboratorio(request.laboratorioId());
        validarDisponibilidad(request.laboratorioId(), request.fecha(),
                request.horaInicio(), request.horaFin());

        solicitud.setEstado(SolicitudReservaStates.desde(solicitud.getEstado())
                .proponerAlternativa());
        solicitud.setPropuestaFecha(request.fecha());
        solicitud.setPropuestaHoraInicio(request.horaInicio());
        solicitud.setPropuestaHoraFin(request.horaFin());
        solicitud.setPropuestaLaboratorioId(request.laboratorioId());
        solicitud.setPropuestaObservacion(request.observacion());
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);
        guardarHistorial(id, EstadoSolicitud.EN_REVISION, EstadoSolicitud.PROPUESTA,
                request.observacion(), actorId);
        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitudReservaResponse aceptarPropuesta(
            UUID id, ResponderPropuestaRequest request, UUID actorId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
        validarPropietario(solicitud, actorId);
        EstadoSolicitud nuevoEstado = SolicitudReservaStates.desde(solicitud.getEstado())
                .aceptarPropuesta();
        validarDisponibilidad(solicitud.getPropuestaLaboratorioId(), solicitud.getPropuestaFecha(),
                solicitud.getPropuestaHoraInicio(), solicitud.getPropuestaHoraFin());
        solicitud.setLaboratorioId(solicitud.getPropuestaLaboratorioId());
        solicitud.setPisoId(politicaAmbito.obtenerPiso(solicitud.getPropuestaLaboratorioId()));
        solicitud.setFechaReserva(solicitud.getPropuestaFecha());
        solicitud.setHoraInicio(solicitud.getPropuestaHoraInicio());
        solicitud.setHoraFin(solicitud.getPropuestaHoraFin());
        solicitud.setEstado(nuevoEstado);
        limpiarPropuesta(solicitud);
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);
        guardarHistorial(id, EstadoSolicitud.PROPUESTA, EstadoSolicitud.EN_REVISION,
                request.comentario(), actorId);
        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public SolicitudReservaResponse rechazarPropuesta(
            UUID id, ResponderPropuestaRequest request, UUID actorId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la solicitud de reserva indicada"));
        validarPropietario(solicitud, actorId);
        solicitud.setEstado(SolicitudReservaStates.desde(solicitud.getEstado())
                .rechazarPropuesta());
        limpiarPropuesta(solicitud);
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);
        guardarHistorial(id, EstadoSolicitud.PROPUESTA, EstadoSolicitud.EN_REVISION,
                request.comentario(), actorId);
        return solicitudReservaMapper.toResponse(guardada);
    }

    private void limpiarPropuesta(SolicitudReserva solicitud) {
        solicitud.setPropuestaFecha(null);
        solicitud.setPropuestaHoraInicio(null);
        solicitud.setPropuestaHoraFin(null);
        solicitud.setPropuestaLaboratorioId(null);
        solicitud.setPropuestaObservacion(null);
    }

    private void guardarHistorial(UUID solicitudId, EstadoSolicitud anterior,
                                  EstadoSolicitud nuevo, String comentario, UUID actorId) {
        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(solicitudId);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setComentario(comentario);
        historial.setUsuarioAccionId(actorId);
        historialSolicitudRepository.guardar(historial);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<HistorialSolicitudResponse> obtenerHistorial(
            UUID solicitudId, int pagina, int tamanio) {
        Pagina<HistorialSolicitud> historial =
                historialSolicitudRepository.buscarPorSolicitudId(solicitudId, pagina, tamanio);
        return new PaginaResponse<>(
                historial.contenido().stream()
                        .map(historialSolicitudMapper::toResponse)
                        .toList(),
                historial.numero(), historial.tamanio(), historial.totalElementos(),
                historial.totalPaginas(), historial.primera(), historial.ultima());
    }

    private PaginaResponse<SolicitudReservaResponse> mapearPagina(Pagina<SolicitudReserva> pagina) {
        return new PaginaResponse<>(pagina.contenido().stream().map(solicitudReservaMapper::toResponse).toList(),
                pagina.numero(), pagina.tamanio(), pagina.totalElementos(), pagina.totalPaginas(),
                pagina.primera(), pagina.ultima());
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
