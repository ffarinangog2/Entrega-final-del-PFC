package ec.edu.scli.reservas.service.impl;

import ec.edu.scli.reservas.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.entity.Reserva;
import ec.edu.scli.reservas.enums.EstadoReserva;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.repository.ReservaRepository;
import ec.edu.scli.reservas.service.ReservaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Implementa las operaciones de negocio de las reservas confirmadas. */
@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ReservaMapper reservaMapper;

    public ReservaServiceImpl(ReservaRepository reservaRepository, ReservaMapper reservaMapper) {
        this.reservaRepository = reservaRepository;
        this.reservaMapper = reservaMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> listar(
            EstadoReserva estado,
            UUID laboratorioId,
            UUID responsableId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int pagina,
            int tamanio) {
        Pageable pageable = PageRequest.of(pagina, tamanio);
        boolean existenFiltros = estado != null
                || laboratorioId != null
                || responsableId != null
                || fechaDesde != null
                || fechaHasta != null;

        Page<Reserva> reservas;
        if (!existenFiltros) {
            reservas = reservaRepository.findAll(pageable);
        } else {
            Specification<Reserva> specification = Specification.allOf(
                    igual("estado", estado),
                    igual("laboratorioId", laboratorioId),
                    igual("responsableId", responsableId),
                    fechaDesde(fechaDesde),
                    fechaHasta(fechaHasta));
            reservas = reservaRepository.findAll(specification, pageable);
        }
        return mapearPagina(reservas);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse buscarPorId(UUID id) {
        return reservaMapper.toResponse(obtenerReserva(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> listarPorLaboratorio(
            UUID laboratorioId, int pagina, int tamanio) {
        return mapearPagina(reservaRepository.findByLaboratorioId(
                laboratorioId, PageRequest.of(pagina, tamanio)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> listarPorResponsable(
            UUID responsableId, int pagina, int tamanio) {
        return mapearPagina(reservaRepository.findByResponsableId(
                responsableId, PageRequest.of(pagina, tamanio)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReservaResponse> obtenerCalendario(
            UUID laboratorioId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int pagina,
            int tamanio) {
        Pageable pageable = PageRequest.of(pagina, tamanio);
        if (laboratorioId != null) {
            return mapearPagina(reservaRepository.findByLaboratorioIdAndFechaReservaBetween(
                    laboratorioId, fechaDesde, fechaHasta, pageable));
        }

        Specification<Reserva> specification = Specification.allOf(
                fechaDesde(fechaDesde), fechaHasta(fechaHasta));
        return mapearPagina(reservaRepository.findAll(specification, pageable));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse cancelar(
            UUID id, CancelarReservaRequest request, UUID usuarioAutenticadoId) {
        Reserva reserva = obtenerReservaParaActualizar(id);
        if (reserva.getEstado() != EstadoReserva.PROGRAMADA) {
            throw new IllegalStateException(
                    "La reserva solamente puede cancelarse cuando está programada");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse iniciar(UUID id, UUID usuarioAutenticadoId) {
        Reserva reserva = obtenerReservaParaActualizar(id);
        if (reserva.getEstado() != EstadoReserva.PROGRAMADA) {
            throw new IllegalStateException(
                    "La reserva solamente puede iniciar cuando está programada");
        }

        LocalDateTime inicioPermitido =
                LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraInicio());
        if (LocalDateTime.now().isBefore(inicioPermitido)) {
            throw new IllegalStateException(
                    "La reserva no puede iniciar antes de la fecha y hora programadas");
        }

        reserva.setEstado(EstadoReserva.EN_CURSO);
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse finalizar(UUID id, UUID usuarioAutenticadoId) {
        Reserva reserva = obtenerReservaParaActualizar(id);
        if (reserva.getEstado() != EstadoReserva.EN_CURSO) {
            throw new IllegalStateException(
                    "La reserva solamente puede finalizar cuando está en curso");
        }

        reserva.setEstado(EstadoReserva.FINALIZADA);
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponse marcarNoAsistida(UUID id, UUID usuarioAutenticadoId) {
        Reserva reserva = obtenerReservaParaActualizar(id);
        if (reserva.getEstado() != EstadoReserva.PROGRAMADA) {
            throw new IllegalStateException(
                    "La reserva solamente puede marcarse como no asistida cuando está programada");
        }

        LocalDateTime finProgramado =
                LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraFin());
        if (LocalDateTime.now().isBefore(finProgramado)) {
            throw new IllegalStateException(
                    "La reserva no puede marcarse como no asistida antes de finalizar su franja");
        }

        reserva.setEstado(EstadoReserva.NO_ASISTIDA);
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    private Reserva obtenerReserva(UUID id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la reserva indicada"));
    }

    private Reserva obtenerReservaParaActualizar(UUID id) {
        return reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la reserva indicada"));
    }

    private <T> Specification<Reserva> igual(String atributo, T valor) {
        return valor == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(atributo), valor);
    }

    private Specification<Reserva> fechaDesde(LocalDate fechaDesde) {
        return fechaDesde == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("fechaReserva"), fechaDesde);
    }

    private Specification<Reserva> fechaHasta(LocalDate fechaHasta) {
        return fechaHasta == null ? null : (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("fechaReserva"), fechaHasta);
    }

    private PaginaResponse<ReservaResponse> mapearPagina(Page<Reserva> pagina) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(reservaMapper::toResponse).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
