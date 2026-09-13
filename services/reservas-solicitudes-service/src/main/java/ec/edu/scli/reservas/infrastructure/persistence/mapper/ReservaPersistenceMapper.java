package ec.edu.scli.reservas.infrastructure.persistence.mapper;

import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservaPersistenceMapper {
    public Reserva toDomain(ReservaJpaEntity e) {
        Reserva d = new Reserva();
        d.setId(e.getId()); d.setSolicitudId(e.getSolicitud().getId()); d.setLaboratorioId(e.getLaboratorioId()); d.setPisoId(e.getPisoId());
        d.setResponsableId(e.getResponsableId()); d.setFechaReserva(e.getFechaReserva()); d.setHoraInicio(e.getHoraInicio());
        d.setHoraFin(e.getHoraFin()); d.setEstado(e.getEstado()); d.setCodigoReserva(e.getCodigoReserva());
        d.setCreadaEn(e.getCreadaEn()); d.setActualizadaEn(e.getActualizadaEn()); d.setVersion(e.getVersion());
        return d;
    }
    public void updateEntity(Reserva d, ReservaJpaEntity e) {
        e.setLaboratorioId(d.getLaboratorioId()); e.setPisoId(d.getPisoId()); e.setResponsableId(d.getResponsableId()); e.setFechaReserva(d.getFechaReserva());
        e.setHoraInicio(d.getHoraInicio()); e.setHoraFin(d.getHoraFin()); e.setEstado(d.getEstado()); e.setCodigoReserva(d.getCodigoReserva());
    }
}
