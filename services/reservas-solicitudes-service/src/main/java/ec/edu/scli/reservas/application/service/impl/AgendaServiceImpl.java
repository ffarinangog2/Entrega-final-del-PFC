package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.presentation.dto.request.CrearBloqueoAgendaRequest;
import ec.edu.scli.reservas.presentation.dto.response.AgendaItemResponse;
import ec.edu.scli.reservas.presentation.dto.response.BloqueoAgendaResponse;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.mapper.BloqueoAgendaMapper;
import ec.edu.scli.reservas.domain.port.out.BloqueoAgendaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.application.service.AgendaService;
import org.springframework.beans.BeanUtils;
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

    private final ReservaRepositoryPort reservaRepository;
    private final BloqueoAgendaRepositoryPort bloqueoAgendaRepository;
    private final BloqueoAgendaMapper bloqueoAgendaMapper;
    private final AcademicoLaboratoriosClient academicoLaboratoriosClient;
    private final TransactionTemplate transactionTemplate;

    public AgendaServiceImpl(
            ReservaRepositoryPort reservaRepository,
            BloqueoAgendaRepositoryPort bloqueoAgendaRepository,
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

        List<AgendaItemResponse> elementos = new ArrayList<>();
        reservaRepository.buscarParaAgenda(laboratorioId, fechaDesde, fechaHasta).stream()
                .map(this::mapearReserva)
                .forEach(elementos::add);
        bloqueoAgendaRepository.buscarActivos(laboratorioId, fechaDesde, fechaHasta).stream()
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
                bloqueoAgendaRepository.contarActivosConflictivos(
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

        return bloqueoAgendaMapper.toResponse(bloqueoAgendaRepository.guardar(bloqueo));
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

        BloqueoAgenda bloqueo = bloqueoAgendaRepository.buscarPorId(bloqueoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El bloqueo de agenda no existe"));
        if (Boolean.FALSE.equals(bloqueo.getActivo())) {
            return;
        }

        bloqueo.setActivo(false);
        bloqueoAgendaRepository.guardar(bloqueo);
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

}
