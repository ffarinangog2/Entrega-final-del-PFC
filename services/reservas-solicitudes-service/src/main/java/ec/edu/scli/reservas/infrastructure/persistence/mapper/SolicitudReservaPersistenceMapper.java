package ec.edu.scli.reservas.infrastructure.persistence.mapper;

import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudReservaJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SolicitudReservaPersistenceMapper {
    public SolicitudReserva toDomain(SolicitudReservaJpaEntity e) {
        SolicitudReserva d = new SolicitudReserva();
        d.setId(e.getId()); d.setSolicitanteId(e.getSolicitanteId()); d.setDocenteId(e.getDocenteId());
        d.setLaboratorioId(e.getLaboratorioId()); d.setPisoId(e.getPisoId()); d.setMateriaId(e.getMateriaId()); d.setPeriodoLectivoId(e.getPeriodoLectivoId());
        d.setFechaReserva(e.getFechaReserva()); d.setHoraInicio(e.getHoraInicio()); d.setHoraFin(e.getHoraFin());
        d.setNumeroParticipantes(e.getNumeroParticipantes()); d.setMotivo(e.getMotivo()); d.setObservacion(e.getObservacion());
        d.setEstado(e.getEstado()); d.setClaveIdempotencia(e.getClaveIdempotencia()); d.setCreadaEn(e.getCreadaEn());
        d.setActualizadaEn(e.getActualizadaEn()); d.setVersion(e.getVersion());
        d.setReservaId(e.getReserva() == null ? null : e.getReserva().getId());
        d.setPropuestaFecha(e.getPropuestaFecha()); d.setPropuestaHoraInicio(e.getPropuestaHoraInicio());
        d.setPropuestaHoraFin(e.getPropuestaHoraFin()); d.setPropuestaLaboratorioId(e.getPropuestaLaboratorioId());
        d.setPropuestaObservacion(e.getPropuestaObservacion());
        return d;
    }
    public void updateEntity(SolicitudReserva d, SolicitudReservaJpaEntity e) {
        e.setSolicitanteId(d.getSolicitanteId()); e.setDocenteId(d.getDocenteId()); e.setLaboratorioId(d.getLaboratorioId()); e.setPisoId(d.getPisoId());
        e.setMateriaId(d.getMateriaId()); e.setPeriodoLectivoId(d.getPeriodoLectivoId()); e.setFechaReserva(d.getFechaReserva());
        e.setHoraInicio(d.getHoraInicio()); e.setHoraFin(d.getHoraFin()); e.setNumeroParticipantes(d.getNumeroParticipantes());
        e.setMotivo(d.getMotivo()); e.setObservacion(d.getObservacion()); e.setEstado(d.getEstado()); e.setClaveIdempotencia(d.getClaveIdempotencia());
        e.setPropuestaFecha(d.getPropuestaFecha()); e.setPropuestaHoraInicio(d.getPropuestaHoraInicio());
        e.setPropuestaHoraFin(d.getPropuestaHoraFin()); e.setPropuestaLaboratorioId(d.getPropuestaLaboratorioId());
        e.setPropuestaObservacion(d.getPropuestaObservacion());
    }
}
