package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.ExisteExternoResponse;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.client.dto.PerfilExternoResponse;
import ec.edu.scli.reservas.presentation.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.presentation.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudReservaResponse;
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
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.domain.state.reserva.ReservaStates;
import ec.edu.scli.reservas.domain.state.solicitud.SolicitudReservaStates;
import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Implementa las operaciones de negocio de las solicitudes de reserva. */
@Service
public class SolicitudReservaServiceImpl implements SolicitudReservaService {

    private final SolicitudReservaRepositoryPort solicitudReservaRepository;
    private final ReservaRepositoryPort reservaRepository;
    private final HistorialSolicitudRepositoryPort historialSolicitudRepository;
    private final SolicitudReservaMapper solicitudReservaMapper;
    private final ReservaMapper reservaMapper;
    private final HistorialSolicitudMapper historialSolicitudMapper;
    private final UsuariosClient usuariosClient;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final DisponibilidadService disponibilidadService;
    private final BusinessEventMetrics businessEventMetrics;

    public SolicitudReservaServiceImpl(
            SolicitudReservaRepositoryPort solicitudReservaRepository,
            ReservaRepositoryPort reservaRepository,
            HistorialSolicitudRepositoryPort historialSolicitudRepository,
            SolicitudReservaMapper solicitudReservaMapper,
            ReservaMapper reservaMapper,
            HistorialSolicitudMapper historialSolicitudMapper,
            UsuariosClient usuariosClient,
            AcademicoLaboratoriosClient academicoLaboratoriosClient,
            DisponibilidadService disponibilidadService,
            BusinessEventMetrics businessEventMetrics) {
        this.solicitudReservaRepository = solicitudReservaRepository;
        this.reservaRepository = reservaRepository;
        this.historialSolicitudRepository = historialSolicitudRepository;
        this.solicitudReservaMapper = solicitudReservaMapper;
        this.reservaMapper = reservaMapper;
        this.historialSolicitudMapper = historialSolicitudMapper;
        this.usuariosClient = usuariosClient;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
        this.disponibilidadService = disponibilidadService;
        this.businessEventMetrics = businessEventMetrics;
    }

    @Override
    @Transactional
    public SolicitudReservaResponse crear(
            CrearSolicitudReservaRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        return solicitudReservaRepository .buscarPorClaveIdempotencia(claveIdempotencia)
                .map(solicitudReservaMapper::toResponse)
                .orElseGet(() -> crearNuevaSolicitud(request, claveIdempotencia, usuarioAutenticadoId));
    }

    private SolicitudReservaResponse crearNuevaSolicitud(
            CrearSolicitudReservaRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        validarDocente(request.docenteId());
        validarLaboratorio(request.laboratorioId());
        validarMateria(request.materiaId());
        validarPeriodoLectivo(request.periodoLectivoId());
        validarDisponibilidad(
                request.laboratorioId(), request.fechaReserva(), request.horaInicio(), request.horaFin());

        SolicitudReserva solicitud = BeanUtils.instantiateClass(SolicitudReserva.class);
        solicitud.setSolicitanteId(request.solicitanteId());
        solicitud.setDocenteId(request.docenteId());
        solicitud.setLaboratorioId(request.laboratorioId());
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
                new FiltroSolicitudReserva(estado, solicitanteId, laboratorioId, fecha), pagina, tamanio));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudReservaResponse buscarPorId(UUID id) {
        return solicitudReservaMapper.toResponse(obtenerSolicitud(id));
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
        SolicitudReservaStates.desde(solicitud.getEstado()).validarActualizacion();

        validarDocente(request.docenteId());
        validarLaboratorio(request.laboratorioId());
        validarMateria(request.materiaId());
        validarPeriodoLectivo(request.periodoLectivoId());
        validarDisponibilidad(
                request.laboratorioId(), request.fechaReserva(), request.horaInicio(), request.horaFin());

        solicitud.setDocenteId(request.docenteId());
        solicitud.setLaboratorioId(request.laboratorioId());
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

    private SolicitudReserva obtenerSolicitud(UUID id) {
        return solicitudReservaRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));
    }

    private void validarDocente(UUID docenteId) {
        PerfilExternoResponse docente = usuariosClient.obtenerPerfil(docenteId);
        if (docente == null || !docente.existe()) {
            throw new IllegalArgumentException("El docente indicado no existe");
        }
        if (!docente.activo()) {
            throw new IllegalArgumentException("El docente indicado no está activo");
        }
        if (docente.tiposPerfil() == null
                || docente.tiposPerfil().stream().noneMatch("DOCENTE"::equalsIgnoreCase)) {
            throw new IllegalArgumentException("El perfil indicado no corresponde a un docente");
        }
    }

    private void validarLaboratorio(UUID laboratorioId) {
        LaboratorioExternoResponse laboratorio =
                academicoLaboratoriosClient.obtenerLaboratorio(laboratorioId);
        if (laboratorio == null || !laboratorio.existe()) {
            throw new IllegalArgumentException("El laboratorio indicado no existe");
        }
        if (!laboratorio.activo()) {
            throw new IllegalArgumentException("El laboratorio indicado no está activo");
        }
    }

    private void validarMateria(UUID materiaId) {
        ExisteExternoResponse materia = academicoLaboratoriosClient.verificarMateria(materiaId);
        if (materia == null || !materia.existe()) {
            throw new IllegalArgumentException("La materia indicada no existe");
        }
    }

    private void validarPeriodoLectivo(UUID periodoLectivoId) {
        ExisteExternoResponse periodo =
                academicoLaboratoriosClient.verificarPeriodoLectivo(periodoLectivoId);
        if (periodo == null || !periodo.existe()) {
            throw new IllegalArgumentException("El período lectivo indicado no existe");
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));

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
    public ReservaResponse aprobar(
            UUID id,
            AprobarSolicitudRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));

        EstadoSolicitud estadoAprobado = SolicitudReservaStates.desde(solicitud.getEstado()).aprobar();

        if (reservaRepository.existePorSolicitudId(id)) {
            throw new IllegalStateException(
                    "La solicitud ya tiene una reserva asociada");
        }

        validarDisponibilidad(
                solicitud.getLaboratorioId(),
                solicitud.getFechaReserva(),
                solicitud.getHoraInicio(),
                solicitud.getHoraFin());
        validarLaboratorio(solicitud.getLaboratorioId());

        Reserva reserva = BeanUtils.instantiateClass(Reserva.class);
        reserva.setSolicitudId(solicitud.getId());
        reserva.setLaboratorioId(solicitud.getLaboratorioId());
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

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(solicitud.getId());
        historial.setEstadoAnterior(EstadoSolicitud.EN_REVISION);
        historial.setEstadoNuevo(EstadoSolicitud.APROBADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudAprobada();
        businessEventMetrics.reservaCreada();
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));
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
        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitudReservaResponse cancelar(
            UUID id, CancelarSolicitudRequest request, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));
        EstadoSolicitud estadoCancelado = SolicitudReservaStates.desde(solicitud.getEstado()).cancelar();

        Reserva reservaAsociada = reservaRepository.buscarPorSolicitudId(id)
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud aprobada no tiene una reserva asociada"));
        Reserva reserva = reservaRepository.buscarPorIdParaActualizar(reservaAsociada.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud aprobada no tiene una reserva asociada"));
        reserva.setEstado(ReservaStates.desde(reserva.getEstado()).cancelar());
        reservaRepository.guardar(reserva);

        solicitud.setEstado(estadoCancelado);
        SolicitudReserva guardada = solicitudReservaRepository.guardar(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitudId(guardada.getId());
        historial.setEstadoAnterior(EstadoSolicitud.APROBADA);
        historial.setEstadoNuevo(EstadoSolicitud.CANCELADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.guardar(historial);

        businessEventMetrics.solicitudCancelada();
        businessEventMetrics.reservaCancelada();
        return solicitudReservaMapper.toResponse(guardada);
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
}
