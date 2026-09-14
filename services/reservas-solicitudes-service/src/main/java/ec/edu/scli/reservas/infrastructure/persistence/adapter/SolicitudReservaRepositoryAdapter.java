package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudReservaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.SolicitudReservaPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SolicitudReservaSpringDataRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import java.util.*;
import java.time.Instant;

@Component
public class SolicitudReservaRepositoryAdapter implements SolicitudReservaRepositoryPort {
    private final SolicitudReservaSpringDataRepository repository; private final SolicitudReservaPersistenceMapper mapper;
    public SolicitudReservaRepositoryAdapter(SolicitudReservaSpringDataRepository r,SolicitudReservaPersistenceMapper m){repository=r;mapper=m;}
    public Pagina<SolicitudReserva> buscar(FiltroSolicitudReserva f,int p,int t){
        boolean vacio=f.estado()==null&&f.solicitanteId()==null&&f.laboratorioId()==null&&f.pisoId()==null&&f.fecha()==null;
        Specification<SolicitudReservaJpaEntity>s=Specification.allOf(igual("estado",f.estado()),igual("solicitanteId",f.solicitanteId()),igual("laboratorioId",f.laboratorioId()),igual("pisoId",f.pisoId()),igual("fechaReserva",f.fecha()));
        return pagina(vacio?repository.findAll(PageRequest.of(p,t)):repository.findAll(s,PageRequest.of(p,t)));
    }
    public Pagina<SolicitudReserva> buscarPorSolicitante(UUID id,int p,int t){return pagina(repository.findBySolicitanteId(id,PageRequest.of(p,t)));}
    public Pagina<SolicitudReserva> buscarPorEstado(EstadoSolicitud e,int p,int t){return pagina(repository.findByEstado(e,PageRequest.of(p,t)));}
    public Optional<SolicitudReserva> buscarPorId(UUID id){return repository.findById(id).map(mapper::toDomain);}
    public Optional<SolicitudReserva> buscarPorIdParaActualizar(UUID id){return repository.findByIdForUpdate(id).map(mapper::toDomain);}
    public Optional<SolicitudReserva> buscarPorClaveIdempotencia(String c){return repository.findByClaveIdempotencia(c).map(mapper::toDomain);}
    public SolicitudReserva guardar(SolicitudReserva d){
        SolicitudReservaJpaEntity e=d.getId()==null?new SolicitudReservaJpaEntity():repository.findById(d.getId()).orElseThrow();
        if (d.getId() != null && !Objects.equals(d.getVersion(), e.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(SolicitudReservaJpaEntity.class, d.getId());
        }
        mapper.updateEntity(d,e); return mapper.toDomain(repository.save(e));
    }
    public List<SolicitudReserva> buscarPendientesAnterioresA(Instant limite){
        return repository.findByEstadoAndCreadaEnBefore(EstadoSolicitud.PENDIENTE, limite).stream().map(mapper::toDomain).toList();
    }
    private <T>Specification<SolicitudReservaJpaEntity>igual(String a,T v){return v==null?null:(r,q,b)->b.equal(r.get(a),v);}
    private Pagina<SolicitudReserva> pagina(Page<SolicitudReservaJpaEntity>p){return new Pagina<>(p.stream().map(mapper::toDomain).toList(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages(),p.isFirst(),p.isLast());}
}
