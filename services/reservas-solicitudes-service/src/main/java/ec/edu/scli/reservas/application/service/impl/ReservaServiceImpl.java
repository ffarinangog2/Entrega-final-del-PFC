package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.application.service.ReservaService;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.domain.state.reserva.ReservaStates;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.observability.BusinessEventMetrics;
import ec.edu.scli.reservas.presentation.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.presentation.dto.response.*;
import ec.edu.scli.reservas.presentation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.UUID;

@Service
@Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3, backoff = @Backoff(delay = 25, multiplier = 2, maxDelay = 100))
public class ReservaServiceImpl implements ReservaService {
    private final ReservaRepositoryPort reservaRepository;
    private final ReservaMapper reservaMapper;
    private final BusinessEventMetrics businessEventMetrics;
    private final PoliticaAmbitoLaboratorio politicaAmbito;
    private final SolicitudReservaRepositoryPort solicitudRepository;
    public ReservaServiceImpl(
            ReservaRepositoryPort r, ReservaMapper m, BusinessEventMetrics metrics,
            PoliticaAmbitoLaboratorio politicaAmbito,
            SolicitudReservaRepositoryPort solicitudRepository) {
        reservaRepository = r;
        reservaMapper = m;
        businessEventMetrics = metrics;
        this.politicaAmbito = politicaAmbito;
        this.solicitudRepository = solicitudRepository;
    }
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listar(EstadoReserva e,UUID l,UUID r,LocalDate d,LocalDate h,int p,int t){
        ActorAutenticado actor = politicaAmbito.actor();
        UUID piso = null;
        UUID solicitante = null;
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) piso = politicaAmbito.pisoGestionado();
        else if (!esGestorGlobal(actor)) solicitante = actor.perfilId();
        return mapearPagina(reservaRepository.buscar(new FiltroReserva(e,l,r,piso,solicitante,d,h),p,t));
    }
    @Override @Transactional(readOnly=true)
    public ReservaResponse buscarPorId(UUID id){Reserva r=obtenerReserva(id); validarLectura(r); return reservaMapper.toResponse(r);}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listarPorLaboratorio(UUID id,int p,int t){return listar(null,id,null,null,null,p,t);}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> listarPorResponsable(UUID id,int p,int t){return listar(null,null,id,null,null,p,t);}
    @Override @Transactional(readOnly=true)
    public PaginaResponse<ReservaResponse> obtenerCalendario(UUID id,LocalDate d,LocalDate h,int p,int t){return listar(null,id,null,d,h,p,t);}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse cancelar(UUID id,CancelarReservaRequest request,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);politicaAmbito.validarGestion(r.getLaboratorioId());r.setEstado(ReservaStates.desde(r.getEstado()).cancelar());ReservaResponse response=reservaMapper.toResponse(reservaRepository.guardar(r));businessEventMetrics.reservaCancelada();return response;}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse iniciar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);politicaAmbito.validarGestion(r.getLaboratorioId());r.setEstado(ReservaStates.desde(r.getEstado()).iniciar(r,LocalDateTime.now()));return reservaMapper.toResponse(reservaRepository.guardar(r));}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse finalizar(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);politicaAmbito.validarGestion(r.getLaboratorioId());r.setEstado(ReservaStates.desde(r.getEstado()).finalizar());ReservaResponse response=reservaMapper.toResponse(reservaRepository.guardar(r));businessEventMetrics.reservaFinalizada();return response;}
    @Override @Transactional(isolation=Isolation.SERIALIZABLE)
    public ReservaResponse marcarNoAsistida(UUID id,UUID usuario){Reserva r=obtenerReservaParaActualizar(id);politicaAmbito.validarGestion(r.getLaboratorioId());r.setEstado(ReservaStates.desde(r.getEstado()).marcarNoAsistida(r,LocalDateTime.now()));return reservaMapper.toResponse(reservaRepository.guardar(r));}
    private Reserva obtenerReserva(UUID id){return reservaRepository.buscarPorId(id).orElseThrow(()->new ResourceNotFoundException("No existe la reserva indicada"));}
    private Reserva obtenerReservaParaActualizar(UUID id){return reservaRepository.buscarPorIdParaActualizar(id).orElseThrow(()->new ResourceNotFoundException("No existe la reserva indicada"));}
    private void validarLectura(Reserva reserva) {
        ActorAutenticado actor = politicaAmbito.actor();
        if (esGestorGlobal(actor)) return;
        if (actor.tiene("ROLE_ADMINISTRADOR_PISO")) { politicaAmbito.validarGestion(reserva.getLaboratorioId()); return; }
        SolicitudReserva solicitud = solicitudRepository.buscarPorId(reserva.getSolicitudId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la solicitud asociada"));
        if (!actor.perfilId().equals(solicitud.getSolicitanteId())) {
            throw new AccessDeniedException("La reserva no pertenece al actor autenticado");
        }
    }
    private boolean esGestorGlobal(ActorAutenticado actor) {
        return actor.tiene("ROLE_ADMINISTRADOR") || actor.tiene("ROLE_TECNICO");
    }
    private PaginaResponse<ReservaResponse> mapearPagina(Pagina<Reserva> p){return new PaginaResponse<>(p.contenido().stream().map(reservaMapper::toResponse).toList(),p.numero(),p.tamanio(),p.totalElementos(),p.totalPaginas(),p.primera(),p.ultima());}
}
