package ec.edu.scli.reservas.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.UsuariosClient;
import ec.edu.scli.reservas.client.dto.ExisteExternoResponse;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.client.dto.PerfilExternoResponse;
import ec.edu.scli.reservas.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.entity.HistorialSolicitud;
import ec.edu.scli.reservas.entity.Reserva;
import ec.edu.scli.reservas.entity.SolicitudReserva;
import ec.edu.scli.reservas.enums.EstadoReserva;
import ec.edu.scli.reservas.enums.EstadoSolicitud;
import ec.edu.scli.reservas.mapper.HistorialSolicitudMapper;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.mapper.SolicitudReservaMapper;
import ec.edu.scli.reservas.repository.HistorialSolicitudRepository;
import ec.edu.scli.reservas.repository.ReservaRepository;
import ec.edu.scli.reservas.repository.SolicitudReservaRepository;
import ec.edu.scli.reservas.service.DisponibilidadService;
import ec.edu.scli.reservas.service.SolicitudReservaService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Implementa las operaciones de negocio de las solicitudes de reserva. */
@Service
public class SolicitudReservaServiceImpl implements SolicitudReservaService {

    private final SolicitudReservaRepository solicitudReservaRepository;
    private final ReservaRepository reservaRepository;
    private final HistorialSolicitudRepository historialSolicitudRepository;
    private final SolicitudReservaMapper solicitudReservaMapper;
    private final ReservaMapper reservaMapper;
    private final HistorialSolicitudMapper historialSolicitudMapper;
    private final UsuariosClient usuariosClient;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final DisponibilidadService disponibilidadService;

    public SolicitudReservaServiceImpl(
            SolicitudReservaRepository solicitudReservaRepository,
            ReservaRepository reservaRepository,
            HistorialSolicitudRepository historialSolicitudRepository,
            SolicitudReservaMapper solicitudReservaMapper,
            ReservaMapper reservaMapper,
            HistorialSolicitudMapper historialSolicitudMapper,
            UsuariosClient usuariosClient,
            AcademicoLaboratoriosClient academicoLaboratoriosClient,
            DisponibilidadService disponibilidadService) {
        this.solicitudReservaRepository = solicitudReservaRepository;
        this.reservaRepository = reservaRepository;
        this.historialSolicitudRepository = historialSolicitudRepository;
        this.solicitudReservaMapper = solicitudReservaMapper;
        this.reservaMapper = reservaMapper;
        this.historialSolicitudMapper = historialSolicitudMapper;
        this.usuariosClient = usuariosClient;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
        this.disponibilidadService = disponibilidadService;
    }

    @Override
    @Transactional
    public SolicitudReservaResponse crear(
            CrearSolicitudReservaRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        return solicitudReservaRepository.findByClaveIdempotencia(claveIdempotencia)
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

        SolicitudReserva guardada = solicitudReservaRepository.save(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitud(guardada);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(EstadoSolicitud.PENDIENTE);
        historial.setComentario("Solicitud creada");
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.save(historial);

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
        Pageable pageable = PageRequest.of(pagina, tamanio);
        boolean existenFiltros =
                estado != null || solicitanteId != null || laboratorioId != null || fecha != null;

        Page<SolicitudReserva> solicitudes;
        if (!existenFiltros) {
            solicitudes = solicitudReservaRepository.findAll(pageable);
        } else {
            Specification<SolicitudReserva> specification = Specification.allOf(
                    igual("estado", estado),
                    igual("solicitanteId", solicitanteId),
                    igual("laboratorioId", laboratorioId),
                    igual("fechaReserva", fecha));
            solicitudes = solicitudReservaRepository.findAll(specification, pageable);
        }
        return mapearPagina(solicitudes);
    }

    private <T> Specification<SolicitudReserva> igual(String atributo, T valor) {
        return valor == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(atributo), valor);
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
        return mapearPagina(solicitudReservaRepository.findBySolicitanteId(
                solicitanteId, PageRequest.of(pagina, tamanio)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<SolicitudReservaResponse> listarPorEstado(
            EstadoSolicitud estado, int pagina, int tamanio) {
        return mapearPagina(solicitudReservaRepository.findByEstado(
                estado, PageRequest.of(pagina, tamanio)));
    }

    @Override
    @Transactional
    public SolicitudReservaResponse actualizar(
            UUID id,
            ActualizarSolicitudReservaRequest request,
            UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = obtenerSolicitud(id);
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE
                && solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new IllegalStateException(
                    "La solicitud no puede actualizarse en su estado actual");
        }

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

        return solicitudReservaMapper.toResponse(solicitudReservaRepository.save(solicitud));
    }

    private SolicitudReserva obtenerSolicitud(UUID id) {
        return solicitudReservaRepository.findById(id)
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

    private PaginaResponse<SolicitudReservaResponse> mapearPagina(Page<SolicitudReserva> pagina) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(solicitudReservaMapper::toResponse).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }

    @Override
    @Transactional
    public SolicitudReservaResponse ponerEnRevision(UUID id, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException(
                    "La solicitud solamente puede ponerse en revisión cuando está pendiente");
        }

        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        SolicitudReserva guardada = solicitudReservaRepository.save(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitud(guardada);
        historial.setEstadoAnterior(EstadoSolicitud.PENDIENTE);
        historial.setEstadoNuevo(EstadoSolicitud.EN_REVISION);
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historial.setComentario("Solicitud puesta en revisión");
        historialSolicitudRepository.save(historial);

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse aprobar(
            UUID id,
            AprobarSolicitudRequest request,
            String claveIdempotencia,
            UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));

        if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new IllegalStateException(
                    "La solicitud solamente puede aprobarse cuando está en revisión");
        }

        if (reservaRepository.existsBySolicitudId(id)) {
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
        reserva.setSolicitud(solicitud);
        reserva.setLaboratorioId(solicitud.getLaboratorioId());
        reserva.setResponsableId(usuarioAutenticadoId);
        reserva.setFechaReserva(solicitud.getFechaReserva());
        reserva.setHoraInicio(solicitud.getHoraInicio());
        reserva.setHoraFin(solicitud.getHoraFin());
        reserva.setEstado(EstadoReserva.PROGRAMADA);
        reserva.setCodigoReserva(generarCodigoReserva(solicitud.getFechaReserva()));

        Reserva guardada = reservaRepository.save(reserva);

        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setReserva(guardada);
        solicitudReservaRepository.save(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitud(solicitud);
        historial.setEstadoAnterior(EstadoSolicitud.EN_REVISION);
        historial.setEstadoNuevo(EstadoSolicitud.APROBADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.save(historial);

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
        SolicitudReserva solicitud = solicitudReservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));
        if (solicitud.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new IllegalStateException(
                    "La solicitud solamente puede rechazarse cuando está en revisión");
        }

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        SolicitudReserva guardada = solicitudReservaRepository.save(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitud(guardada);
        historial.setEstadoAnterior(EstadoSolicitud.EN_REVISION);
        historial.setEstadoNuevo(EstadoSolicitud.RECHAZADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.save(historial);

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SolicitudReservaResponse cancelar(
            UUID id, CancelarSolicitudRequest request, UUID usuarioAutenticadoId) {
        SolicitudReserva solicitud = solicitudReservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la solicitud de reserva indicada"));
        if (solicitud.getEstado() != EstadoSolicitud.APROBADA) {
            throw new IllegalStateException(
                    "La solicitud solamente puede cancelarse cuando está aprobada");
        }

        Reserva reservaAsociada = reservaRepository.findBySolicitudId(id)
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud aprobada no tiene una reserva asociada"));
        Reserva reserva = reservaRepository.findByIdForUpdate(reservaAsociada.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud aprobada no tiene una reserva asociada"));
        if (reserva.getEstado() != EstadoReserva.PROGRAMADA) {
            throw new IllegalStateException(
                    "La reserva solamente puede cancelarse cuando está programada");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        SolicitudReserva guardada = solicitudReservaRepository.save(solicitud);

        HistorialSolicitud historial = BeanUtils.instantiateClass(HistorialSolicitud.class);
        historial.setSolicitud(guardada);
        historial.setEstadoAnterior(EstadoSolicitud.APROBADA);
        historial.setEstadoNuevo(EstadoSolicitud.CANCELADA);
        historial.setComentario(request.comentario());
        historial.setUsuarioAccionId(usuarioAutenticadoId);
        historialSolicitudRepository.save(historial);

        return solicitudReservaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<HistorialSolicitudResponse> obtenerHistorial(
            UUID solicitudId, int pagina, int tamanio) {
        Page<HistorialSolicitud> historial =
                historialSolicitudRepository.findBySolicitudIdOrderByFechaHoraAsc(
                        solicitudId, PageRequest.of(pagina, tamanio));
        return new PaginaResponse<>(
                historial.getContent().stream()
                        .map(historialSolicitudMapper::toResponse)
                        .toList(),
                historial.getNumber(),
                historial.getSize(),
                historial.getTotalElements(),
                historial.getTotalPages(),
                historial.isFirst(),
                historial.isLast());
    }
}
