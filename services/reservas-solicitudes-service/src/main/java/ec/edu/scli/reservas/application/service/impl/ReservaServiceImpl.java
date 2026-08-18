package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.application.service.ReservaService;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.state.reserva.ReservaStates;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.observability.BusinessEventMetrics;
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
    private final BusinessEventMetrics businessEventMetrics;
    public ReservaServiceImpl(
            ReservaRepositoryPort r, ReservaMapper m, BusinessEventMetrics metrics) {
        reservaRepository = r;
        reservaMapper = m;
        businessEventMetrics = metrics;
    }
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
    public ReservaResponse cancelar(UUID id,CancelarReservaRequest request,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);r.setEstado(ReservaStates.desde(r.getEstado()).cancelar());ReservaResponse response=reservaMapper.toResponse(reservaRepository.guardar(r));businessEventMetrics.reservaCancelada();return response;}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse iniciar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);r.setEstado(ReservaStates.desde(r.getEstado()).iniciar(r,LocalDateTime.now()));return reservaMapper.toResponse(reservaRepository.guardar(r));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse finalizar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);r.setEstado(ReservaStates.desde(r.getEstado()).finalizar());ReservaResponse response=reservaMapper.toResponse(reservaRepository.guardar(r));businessEventMetrics.reservaFinalizada();return response;}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse marcarNoAsistida(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);r.setEstado(ReservaStates.desde(r.getEstado()).marcarNoAsistida(r,LocalDateTime.now()));return reservaMapper.toResponse(reservaRepository.guardar(r));}
    private Reserva obtenerReserva(UUID id){return reservaRepository.buscarPorId(id).orElseThrow(()->new IllegalArgumentException("No existe la reserva indicada"));}
    private Reserva obtenerReservaParaActualizar(UUID id){return reservaRepository.buscarPorIdParaActualizar(id).orElseThrow(()->new IllegalArgumentException("No existe la reserva indicada"));}
    private PaginaResponse<ReservaResponse> mapearPagina(Pagina<Reserva> p){return new PaginaResponse<>(p.contenido().stream().map(reservaMapper::toResponse).toList(),p.numero(),p.tamanio(),p.totalElementos(),p.totalPaginas(),p.primera(),p.ultima());}
}
