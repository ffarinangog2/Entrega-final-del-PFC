package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.ReservaPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

@Component
public class ReservaRepositoryAdapter implements ReservaRepositoryPort {
    private final ReservaSpringDataRepository repository;
    private final SolicitudReservaSpringDataRepository solicitudRepository;
    private final ReservaPersistenceMapper mapper;
    public ReservaRepositoryAdapter(ReservaSpringDataRepository repository, SolicitudReservaSpringDataRepository solicitudRepository,
                                    ReservaPersistenceMapper mapper) { this.repository=repository; this.solicitudRepository=solicitudRepository; this.mapper=mapper; }

    public Pagina<Reserva> buscar(FiltroReserva f, int p, int t) {
        Specification<ReservaJpaEntity> s = Specification.allOf(igual("estado", f.estado()), igual("laboratorioId", f.laboratorioId()),
                igual("responsableId", f.responsableId()), desde(f.fechaDesde()), hasta(f.fechaHasta()));
        boolean vacio = f.estado()==null && f.laboratorioId()==null && f.responsableId()==null && f.fechaDesde()==null && f.fechaHasta()==null;
        return pagina(vacio ? repository.findAll(PageRequest.of(p,t)) : repository.findAll(s, PageRequest.of(p,t)));
    }
    public List<Reserva> buscarParaAgenda(UUID l, LocalDate d, LocalDate h) {
        return repository.findAll(Specification.allOf(igual("laboratorioId",l), desde(d), hasta(h))).stream().map(mapper::toDomain).toList();
    }
    public Pagina<Reserva> buscarPorLaboratorio(UUID id,int p,int t){return pagina(repository.findByLaboratorioId(id,PageRequest.of(p,t)));}
    public Pagina<Reserva> buscarPorResponsable(UUID id,int p,int t){return pagina(repository.findByResponsableId(id,PageRequest.of(p,t)));}
    public Pagina<Reserva> buscarCalendario(UUID id,LocalDate d,LocalDate h,int p,int t){
        if(id!=null)return pagina(repository.findByLaboratorioIdAndFechaReservaBetween(id,d,h,PageRequest.of(p,t)));
        return pagina(repository.findAll(Specification.allOf(desde(d),hasta(h)),PageRequest.of(p,t)));
    }
    public Optional<Reserva> buscarPorId(UUID id){return repository.findById(id).map(mapper::toDomain);}
    public Optional<Reserva> buscarPorIdParaActualizar(UUID id){return repository.findByIdForUpdate(id).map(mapper::toDomain);}
    public Optional<Reserva> buscarPorSolicitudId(UUID id){return repository.findBySolicitudId(id).map(mapper::toDomain);}
    public boolean existePorSolicitudId(UUID id){return repository.existsBySolicitudId(id);}
    public long contarConflictosActivos(UUID l,LocalDate f,LocalTime i,LocalTime h){return repository.contarConflictosActivos(l,f,i,h);}
    public Reserva guardar(Reserva d){
        ReservaJpaEntity e=d.getId()==null?new ReservaJpaEntity():repository.findById(d.getId()).orElseThrow();
        if (d.getId() != null && !Objects.equals(d.getVersion(), e.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(ReservaJpaEntity.class, d.getId());
        }
        mapper.updateEntity(d,e);
        var solicitud = solicitudRepository.getReferenceById(d.getSolicitudId());
        e.setSolicitud(solicitud);
        solicitud.setReserva(e);
        return mapper.toDomain(repository.save(e));
    }
    private <T> Specification<ReservaJpaEntity> igual(String a,T v){return v==null?null:(r,q,b)->b.equal(r.get(a),v);}
    private Specification<ReservaJpaEntity> desde(LocalDate v){return v==null?null:(r,q,b)->b.greaterThanOrEqualTo(r.get("fechaReserva"),v);}
    private Specification<ReservaJpaEntity> hasta(LocalDate v){return v==null?null:(r,q,b)->b.lessThanOrEqualTo(r.get("fechaReserva"),v);}
    private Pagina<Reserva> pagina(Page<ReservaJpaEntity> p){return new Pagina<>(p.stream().map(mapper::toDomain).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages(),p.isFirst(),p.isLast());}
}
