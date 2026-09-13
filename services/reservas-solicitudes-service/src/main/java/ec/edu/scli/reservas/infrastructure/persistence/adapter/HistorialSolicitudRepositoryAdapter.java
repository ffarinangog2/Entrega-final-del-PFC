package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.HistorialSolicitudRepositoryPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.HistorialSolicitudJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.HistorialSolicitudPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

@Component
public class HistorialSolicitudRepositoryAdapter implements HistorialSolicitudRepositoryPort {
    private final HistorialSolicitudSpringDataRepository repository; private final SolicitudReservaSpringDataRepository solicitudRepository;
    private final HistorialSolicitudPersistenceMapper mapper;
    public HistorialSolicitudRepositoryAdapter(HistorialSolicitudSpringDataRepository r,SolicitudReservaSpringDataRepository s,HistorialSolicitudPersistenceMapper m){repository=r;solicitudRepository=s;mapper=m;}
    public HistorialSolicitud guardar(HistorialSolicitud d){HistorialSolicitudJpaEntity e=d.getId()==null?new HistorialSolicitudJpaEntity():repository.findById(d.getId()).orElseThrow();mapper.updateEntity(d,e);e.setSolicitud(solicitudRepository.getReferenceById(d.getSolicitudId()));return mapper.toDomain(repository.save(e));}
    public Pagina<HistorialSolicitud> buscarPorSolicitudId(java.util.UUID id,int p,int t){Page<HistorialSolicitudJpaEntity>x=repository.findBySolicitudIdOrderByFechaHoraAsc(id,PageRequest.of(p,t));return new Pagina<>(x.stream().map(mapper::toDomain).toList(),x.getNumber(),x.getSize(),x.getTotalElements(),x.getTotalPages(),x.isFirst(),x.isLast());}
}
