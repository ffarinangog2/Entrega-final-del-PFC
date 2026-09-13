package ec.edu.scli.reservas.infrastructure.persistence.mapper;

import ec.edu.scli.reservas.domain.model.HistorialSolicitud;
import ec.edu.scli.reservas.infrastructure.persistence.entity.HistorialSolicitudJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HistorialSolicitudPersistenceMapper {
    public HistorialSolicitud toDomain(HistorialSolicitudJpaEntity e) {
        HistorialSolicitud d = new HistorialSolicitud();
        d.setId(e.getId()); d.setSolicitudId(e.getSolicitud().getId()); d.setEstadoAnterior(e.getEstadoAnterior());
        d.setEstadoNuevo(e.getEstadoNuevo()); d.setUsuarioAccionId(e.getUsuarioAccionId());
        d.setComentario(e.getComentario()); d.setFechaHora(e.getFechaHora()); return d;
    }
    public void updateEntity(HistorialSolicitud d, HistorialSolicitudJpaEntity e) {
        e.setEstadoAnterior(d.getEstadoAnterior()); e.setEstadoNuevo(d.getEstadoNuevo());
        e.setUsuarioAccionId(d.getUsuarioAccionId()); e.setComentario(d.getComentario());
    }
}
