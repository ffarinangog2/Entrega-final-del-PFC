package ec.edu.scli.reservas.infrastructure.persistence.mapper;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMappersTest {

    @Test
    void mapeaReservaEnAmbasDirecciones() {
        UUID solicitudId = UUID.randomUUID();
        SolicitudReservaJpaEntity solicitudEntity = new SolicitudReservaJpaEntity();
        solicitudEntity.setId(solicitudId);
        ReservaJpaEntity entity = new ReservaJpaEntity();
        entity.setId(UUID.randomUUID()); entity.setSolicitud(solicitudEntity);
        entity.setLaboratorioId(UUID.randomUUID()); entity.setResponsableId(UUID.randomUUID());
        entity.setFechaReserva(LocalDate.now()); entity.setHoraInicio(LocalTime.of(8, 0));
        entity.setHoraFin(LocalTime.of(9, 0)); entity.setEstado(EstadoReserva.PROGRAMADA);
        entity.setCodigoReserva("RES-1"); entity.setVersion(2L);
        ReservaPersistenceMapper mapper = new ReservaPersistenceMapper();

        Reserva domain = mapper.toDomain(entity);
        assertEquals(entity.getId(), domain.getId());
        assertEquals(solicitudId, domain.getSolicitudId());
        assertEquals(2L, domain.getVersion());

        domain.setEstado(EstadoReserva.CANCELADA);
        domain.setCodigoReserva("RES-2");
        mapper.updateEntity(domain, entity);
        assertEquals(EstadoReserva.CANCELADA, entity.getEstado());
        assertEquals("RES-2", entity.getCodigoReserva());
    }

    @Test
    void mapeaSolicitudEnAmbasDireccionesIncluidaReserva() {
        SolicitudReservaJpaEntity entity = new SolicitudReservaJpaEntity();
        entity.setId(UUID.randomUUID()); entity.setSolicitanteId(UUID.randomUUID());
        entity.setDocenteId(UUID.randomUUID()); entity.setLaboratorioId(UUID.randomUUID());
        entity.setMateriaId(UUID.randomUUID()); entity.setPeriodoLectivoId(UUID.randomUUID());
        entity.setFechaReserva(LocalDate.now()); entity.setHoraInicio(LocalTime.of(8, 0));
        entity.setHoraFin(LocalTime.of(9, 0)); entity.setNumeroParticipantes(20);
        entity.setMotivo("clase"); entity.setObservacion("obs");
        entity.setEstado(EstadoSolicitud.EN_REVISION); entity.setClaveIdempotencia("clave"); entity.setVersion(3L);
        ReservaJpaEntity reserva = new ReservaJpaEntity(); reserva.setId(UUID.randomUUID());
        entity.setReserva(reserva);
        SolicitudReservaPersistenceMapper mapper = new SolicitudReservaPersistenceMapper();

        SolicitudReserva domain = mapper.toDomain(entity);
        assertEquals(reserva.getId(), domain.getReservaId());
        assertEquals(20, domain.getNumeroParticipantes());
        assertEquals(3L, domain.getVersion());

        domain.setEstado(EstadoSolicitud.APROBADA);
        domain.setObservacion("actualizada");
        mapper.updateEntity(domain, entity);
        assertEquals(EstadoSolicitud.APROBADA, entity.getEstado());
        assertEquals("actualizada", entity.getObservacion());
    }

    @Test
    void mapeaHistorialEnAmbasDirecciones() {
        SolicitudReservaJpaEntity solicitud = new SolicitudReservaJpaEntity();
        solicitud.setId(UUID.randomUUID());
        HistorialSolicitudJpaEntity entity = new HistorialSolicitudJpaEntity();
        entity.setId(UUID.randomUUID()); entity.setSolicitud(solicitud);
        entity.setEstadoAnterior(EstadoSolicitud.PENDIENTE);
        entity.setEstadoNuevo(EstadoSolicitud.EN_REVISION);
        entity.setUsuarioAccionId(UUID.randomUUID()); entity.setComentario("revisión");
        HistorialSolicitudPersistenceMapper mapper = new HistorialSolicitudPersistenceMapper();

        HistorialSolicitud domain = mapper.toDomain(entity);
        assertEquals(solicitud.getId(), domain.getSolicitudId());
        assertEquals(EstadoSolicitud.EN_REVISION, domain.getEstadoNuevo());

        domain.setEstadoNuevo(EstadoSolicitud.RECHAZADA);
        domain.setComentario("rechazada");
        mapper.updateEntity(domain, entity);
        assertEquals(EstadoSolicitud.RECHAZADA, entity.getEstadoNuevo());
        assertEquals("rechazada", entity.getComentario());
    }
}
