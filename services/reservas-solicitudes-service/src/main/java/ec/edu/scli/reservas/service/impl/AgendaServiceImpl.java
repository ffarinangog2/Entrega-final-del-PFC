package ec.edu.scli.reservas.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.dto.request.CrearBloqueoAgendaRequest;
import ec.edu.scli.reservas.dto.response.AgendaItemResponse;
import ec.edu.scli.reservas.dto.response.BloqueoAgendaResponse;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import ec.edu.scli.reservas.entity.Reserva;
import ec.edu.scli.reservas.mapper.BloqueoAgendaMapper;
import ec.edu.scli.reservas.repository.BloqueoAgendaRepository;
import ec.edu.scli.reservas.repository.ReservaRepository;
import ec.edu.scli.reservas.service.AgendaService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Implementa la consulta unificada de agenda y la gestión de sus bloqueos. */
@Service
public class AgendaServiceImpl implements AgendaService {

    private static final Comparator<AgendaItemResponse> ORDEN_AGENDA =
            Comparator.comparing(AgendaItemResponse::fecha)
                    .thenComparing(AgendaItemResponse::horaInicio);

    private final ReservaRepository reservaRepository;
    private final BloqueoAgendaRepository bloqueoAgendaRepository;
    private final BloqueoAgendaMapper bloqueoAgendaMapper;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final TransactionTemplate transactionTemplate;

    public AgendaServiceImpl(
            ReservaRepository reservaRepository,
            BloqueoAgendaRepository bloqueoAgendaRepository,
            BloqueoAgendaMapper bloqueoAgendaMapper,
            AcademicoLaboratoriosClient academicoLaboratoriosClient,
            TransactionTemplate transactionTemplate) {
        this.reservaRepository = reservaRepository;
        this.bloqueoAgendaRepository = bloqueoAgendaRepository;
        this.bloqueoAgendaMapper = bloqueoAgendaMapper;
        this.academicoLaboratoriosClient = academicoLaboratoriosClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AgendaItemResponse> listar(
            UUID laboratorioId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int pagina,
            int tamanio) {
        validarPaginacionYRango(fechaDesde, fechaHasta, pagina, tamanio);

        Specification<Reserva> reservaSpecification = Specification.allOf(
                reservaLaboratorioIgualA(laboratorioId),
                reservaFechaDesde(fechaDesde),
                reservaFechaHasta(fechaHasta));
        Specification<BloqueoAgenda> bloqueoSpecification = Specification.allOf(
                bloqueoActivo(),
                bloqueoLaboratorioIgualA(laboratorioId),
                bloqueoFechaDesde(fechaDesde),
                bloqueoFechaHasta(fechaHasta));

        List<AgendaItemResponse> elementos = new ArrayList<>();
        reservaRepository.findAll(reservaSpecification).stream()
                .map(this::mapearReserva)
                .forEach(elementos::add);
        bloqueoAgendaRepository.findAll(bloqueoSpecification).stream()
                .map(this::mapearBloqueo)
                .forEach(elementos::add);
        elementos.sort(ORDEN_AGENDA);

        return paginar(elementos, pagina, tamanio);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AgendaItemResponse> listarPorLaboratorio(
            UUID laboratorioId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int pagina,
            int tamanio) {
        if (laboratorioId == null) {
            throw new IllegalArgumentException("El laboratorio es obligatorio");
        }
        return listar(laboratorioId, fechaDesde, fechaHasta, pagina, tamanio);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BloqueoAgendaResponse crearBloqueo(
            CrearBloqueoAgendaRequest request, UUID usuarioAutenticadoId) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de bloqueo es obligatoria");
        }
        if (usuarioAutenticadoId == null) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio");
        }
        validarHorario(request);

        LaboratorioExternoResponse laboratorio =
                academicoLaboratoriosClient.obtenerLaboratorio(request.laboratorioId());
        if (laboratorio == null || !laboratorio.existe()) {
            throw new IllegalArgumentException("El laboratorio indicado no existe");
        }
        if (!laboratorio.activo()) {
            throw new IllegalArgumentException("El laboratorio indicado no está activo");
        }

        return transactionTemplate.execute(status ->
                crearBloqueoEnTransaccion(request, usuarioAutenticadoId));
    }

    private BloqueoAgendaResponse crearBloqueoEnTransaccion(
            CrearBloqueoAgendaRequest request, UUID usuarioAutenticadoId) {
        long bloqueosConflictivos =
                bloqueoAgendaRepository.contarBloqueosActivosConflictivos(
                        request.laboratorioId(),
                        request.fecha(),
                        request.horaInicio(),
                        request.horaFin());
        if (bloqueosConflictivos > 0) {
            throw new IllegalStateException(
                    "Existe un bloqueo de agenda que cruza el horario solicitado");
        }

        long reservasConflictivas = reservaRepository.contarConflictosActivos(
                request.laboratorioId(),
                request.fecha(),
                request.horaInicio(),
                request.horaFin());
        if (reservasConflictivas > 0) {
            throw new IllegalStateException(
                    "Existe una reserva activa que cruza el horario solicitado");
        }

        BloqueoAgenda bloqueo = BeanUtils.instantiateClass(BloqueoAgenda.class);
        bloqueo.setLaboratorioId(request.laboratorioId());
        bloqueo.setFecha(request.fecha());
        bloqueo.setHoraInicio(request.horaInicio());
        bloqueo.setHoraFin(request.horaFin());
        bloqueo.setMotivo(request.motivo());
        bloqueo.setCreadoPor(usuarioAutenticadoId);
        bloqueo.setActivo(true);

        return bloqueoAgendaMapper.toResponse(bloqueoAgendaRepository.save(bloqueo));
    }

    @Override
    @Transactional
    public void eliminarBloqueo(UUID bloqueoId, UUID usuarioAutenticadoId) {
        if (bloqueoId == null) {
            throw new IllegalArgumentException("El identificador del bloqueo es obligatorio");
        }
        if (usuarioAutenticadoId == null) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio");
        }

        BloqueoAgenda bloqueo = bloqueoAgendaRepository.findById(bloqueoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El bloqueo de agenda no existe"));
        if (Boolean.FALSE.equals(bloqueo.getActivo())) {
            return;
        }

        bloqueo.setActivo(false);
        bloqueoAgendaRepository.save(bloqueo);
    }

    private void validarPaginacionYRango(
            LocalDate fechaDesde, LocalDate fechaHasta, int pagina, int tamanio) {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser menor que cero");
        }
        if (tamanio < 1 || tamanio > 100) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    private void validarHorario(CrearBloqueoAgendaRequest request) {
        if (request.fecha() == null || request.fecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede estar en el pasado");
        }
        if (request.horaInicio() == null
                || request.horaFin() == null
                || !request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException(
                    "La hora de fin debe ser mayor que la hora de inicio");
        }
    }

    private AgendaItemResponse mapearReserva(Reserva reserva) {
        return new AgendaItemResponse(
                reserva.getId(),
                "RESERVA",
                reserva.getLaboratorioId(),
                reserva.getFechaReserva(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                reserva.getEstado().name(),
                reserva.getCodigoReserva());
    }

    private AgendaItemResponse mapearBloqueo(BloqueoAgenda bloqueo) {
        return new AgendaItemResponse(
                bloqueo.getId(),
                "BLOQUEO",
                bloqueo.getLaboratorioId(),
                bloqueo.getFecha(),
                bloqueo.getHoraInicio(),
                bloqueo.getHoraFin(),
                Boolean.TRUE.equals(bloqueo.getActivo()) ? "ACTIVO" : "INACTIVO",
                bloqueo.getMotivo());
    }

    private PaginaResponse<AgendaItemResponse> paginar(
            List<AgendaItemResponse> elementos, int pagina, int tamanio) {
        int totalElementos = elementos.size();
        int totalPaginas = (int) Math.ceil((double) totalElementos / tamanio);
        long desplazamiento = (long) pagina * tamanio;
        int desde = desplazamiento >= totalElementos ? totalElementos : (int) desplazamiento;
        int hasta = Math.min(desde + tamanio, totalElementos);
        List<AgendaItemResponse> contenido = List.copyOf(elementos.subList(desde, hasta));

        return new PaginaResponse<>(
                contenido,
                pagina,
                tamanio,
                totalElementos,
                totalPaginas,
                pagina == 0,
                totalPaginas == 0 || pagina >= totalPaginas - 1);
    }

    private Specification<Reserva> reservaLaboratorioIgualA(UUID laboratorioId) {
        return laboratorioId == null ? null
                : (Root<Reserva> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.equal(root.get("laboratorioId"), laboratorioId);
    }

    private Specification<Reserva> reservaFechaDesde(LocalDate fechaDesde) {
        return fechaDesde == null ? null
                : (Root<Reserva> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.greaterThanOrEqualTo(root.get("fechaReserva"), fechaDesde);
    }

    private Specification<Reserva> reservaFechaHasta(LocalDate fechaHasta) {
        return fechaHasta == null ? null
                : (Root<Reserva> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.lessThanOrEqualTo(root.get("fechaReserva"), fechaHasta);
    }

    private Specification<BloqueoAgenda> bloqueoActivo() {
        return (Root<BloqueoAgenda> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.isTrue(root.get("activo"));
    }

    private Specification<BloqueoAgenda> bloqueoLaboratorioIgualA(UUID laboratorioId) {
        return laboratorioId == null ? null
                : (Root<BloqueoAgenda> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.equal(root.get("laboratorioId"), laboratorioId);
    }

    private Specification<BloqueoAgenda> bloqueoFechaDesde(LocalDate fechaDesde) {
        return fechaDesde == null ? null
                : (Root<BloqueoAgenda> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.greaterThanOrEqualTo(root.get("fecha"), fechaDesde);
    }

    private Specification<BloqueoAgenda> bloqueoFechaHasta(LocalDate fechaHasta) {
        return fechaHasta == null ? null
                : (Root<BloqueoAgenda> root, CriteriaQuery<?> query, CriteriaBuilder builder) ->
                builder.lessThanOrEqualTo(root.get("fecha"), fechaHasta);
    }
}
