package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.application.service.ReservaService;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.presentation.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.presentation.dto.response.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;

@Service
public class ReservaServiceImpl implements ReservaService {
    private final ReservaRepositoryPort reservaRepository;
    private final ReservaMapper reservaMapper;
    public ReservaServiceImpl(ReservaRepositoryPort r, ReservaMapper m){reservaRepository=r;reservaMapper=m;}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listar(EstadoReserva e,UUID l,UUID r,LocalDate d,LocalDate h,int p,int t){return mapearPagina(reservaRepository.buscar(new FiltroReserva(e,l,r,d,h),p,t));}
    @Override @Transactional(readOnly=true)
    public ReservaResponse buscarPorId(UUID id){return reservaMapper.toResponse(obtenerReserva(id));}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listarPorLaboratorio(UUID id,int p,int t){return mapearPagina(reservaRepository.buscarPorLaboratorio(id,p,t));}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listarPorResponsable(UUID id,int p,int t){return mapearPagina(reservaRepository.buscarPorResponsable(id,p,t));}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> obtenerCalendario(UUID id,LocalDate d,LocalDate h,int p,int t){return mapearPagina(reservaRepository.buscarCalendario(id,d,h,p,t));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse cancelar(UUID id,CancelarReservaRequest request,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);if(r.getEstado()!=EstadoReserva.PROGRAMADA)throw new IllegalStateException("La reserva solamente puede cancelarse cuando está programada");r.setEstado(EstadoReserva.CANCELADA);return reservaMapper.toResponse(reservaRepository.guardar(r));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse iniciar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);if(r.getEstado()!=EstadoReserva.PROGRAMADA)throw new IllegalStateException("La reserva solamente puede iniciar cuando está programada");LocalDateTime inicio=LocalDateTime.of(r.getFechaReserva(),r.getHoraInicio());if(LocalDateTime.now().isBefore(inicio))throw new IllegalStateException("La reserva no puede iniciar antes de la fecha y hora programadas");r.setEstado(EstadoReserva.EN_CURSO);return reservaMapper.toResponse(reservaRepository.guardar(r));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse finalizar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);if(r.getEstado()!=EstadoReserva.EN_CURSO)throw new IllegalStateException("La reserva solamente puede finalizar cuando está en curso");r.setEstado(EstadoReserva.FINALIZADA);return reservaMapper.toResponse(reservaRepository.guardar(r));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse marcarNoAsistida(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);if(r.getEstado()!=EstadoReserva.PROGRAMADA)throw new IllegalStateException("La reserva solamente puede marcarse como no asistida cuando está programada");LocalDateTime fin=LocalDateTime.of(r.getFechaReserva(),r.getHoraFin());if(LocalDateTime.now().isBefore(fin))throw new IllegalStateException("La reserva no puede marcarse como no asistida antes de finalizar su franja");r.setEstado(EstadoReserva.NO_ASISTIDA);return reservaMapper.toResponse(reservaRepository.guardar(r));}
    private Reserva obtenerReserva(UUID id){return reservaRepository.buscarPorId(id).orElseThrow(()->new IllegalArgumentException("No existe la reserva indicada"));}
    private Reserva obtenerReservaParaActualizar(UUID id){return reservaRepository.buscarPorIdParaActualizar(id).orElseThrow(()->new IllegalArgumentException("No existe la reserva indicada"));}
    private PaginaResponse<ReservaResponse> mapearPagina(Pagina<Reserva> p){return new PaginaResponse<>(p.contenido().stream().map(reservaMapper::toResponse).toList(),p.numero(),p.tamanio(),p.totalElementos(),p.totalPaginas(),p.primera(),p.ultima());}
}
