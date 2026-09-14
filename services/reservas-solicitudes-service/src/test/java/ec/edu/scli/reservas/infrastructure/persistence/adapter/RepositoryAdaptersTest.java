package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.HistorialSolicitud;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.model.SolicitudReserva;
import ec.edu.scli.reservas.infrastructure.persistence.entity.HistorialSolicitudJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudReservaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.HistorialSolicitudPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.ReservaPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.mapper.SolicitudReservaPersistenceMapper;
import ec.edu.scli.reservas.infrastructure.persistence.repository.HistorialSolicitudSpringDataRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.ReservaSpringDataRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.SolicitudReservaSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RepositoryAdaptersTest {

    @Test
    void rechazaReservaConVersionObsoleta() {
        UUID id = UUID.randomUUID();
        Reserva domain = new Reserva();
        domain.setId(id);
        domain.setVersion(1L);
        ReservaJpaEntity entity = new ReservaJpaEntity();
        entity.setId(id);
        entity.setVersion(2L);
        ReservaSpringDataRepository repository = mock(ReservaSpringDataRepository.class);
        ReservaPersistenceMapper mapper = mock(ReservaPersistenceMapper.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var adapter = new ReservaRepositoryAdapter(repository, mock(SolicitudReservaSpringDataRepository.class), mapper);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> adapter.guardar(domain));
        verify(mapper, never()).updateEntity(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaSolicitudConVersionObsoleta() {
        UUID id = UUID.randomUUID();
        SolicitudReserva domain = new SolicitudReserva();
        domain.setId(id);
        domain.setVersion(1L);
        SolicitudReservaJpaEntity entity = new SolicitudReservaJpaEntity();
        entity.setId(id);
        entity.setVersion(2L);
        SolicitudReservaSpringDataRepository repository = mock(SolicitudReservaSpringDataRepository.class);
        SolicitudReservaPersistenceMapper mapper = mock(SolicitudReservaPersistenceMapper.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var adapter = new SolicitudReservaRepositoryAdapter(repository, mapper);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> adapter.guardar(domain));
        verify(mapper, never()).updateEntity(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void actualizaHistorialExistenteSinCrearOtraEntidad() {
        UUID id = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        HistorialSolicitud domain = new HistorialSolicitud();
        domain.setId(id);
        domain.setSolicitudId(solicitudId);
        HistorialSolicitudJpaEntity existente = new HistorialSolicitudJpaEntity();
        SolicitudReservaJpaEntity solicitud = new SolicitudReservaJpaEntity();
        HistorialSolicitudSpringDataRepository repository = mock(HistorialSolicitudSpringDataRepository.class);
        SolicitudReservaSpringDataRepository solicitudRepository = mock(SolicitudReservaSpringDataRepository.class);
        HistorialSolicitudPersistenceMapper mapper = mock(HistorialSolicitudPersistenceMapper.class);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(solicitudRepository.getReferenceById(solicitudId)).thenReturn(solicitud);
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toDomain(existente)).thenReturn(domain);
        var adapter = new HistorialSolicitudRepositoryAdapter(repository, solicitudRepository, mapper);

        adapter.guardar(domain);

        verify(mapper).updateEntity(domain, existente);
        verify(repository).save(existente);
        assertSame(solicitud, existente.getSolicitud());
    }

    @Test
    void sincronizaRelacionBidireccionalAlGuardarReserva() {
        UUID solicitudId = UUID.randomUUID();
        Reserva domain = new Reserva();
        domain.setSolicitudId(solicitudId);
        SolicitudReservaJpaEntity solicitud = new SolicitudReservaJpaEntity();
        ReservaSpringDataRepository repository = mock(ReservaSpringDataRepository.class);
        SolicitudReservaSpringDataRepository solicitudRepository = mock(SolicitudReservaSpringDataRepository.class);
        ReservaPersistenceMapper mapper = mock(ReservaPersistenceMapper.class);
        when(solicitudRepository.getReferenceById(solicitudId)).thenReturn(solicitud);
        when(repository.save(any(ReservaJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDomain(any())).thenReturn(domain);
        var adapter = new ReservaRepositoryAdapter(repository, solicitudRepository, mapper);

        adapter.guardar(domain);

        var captor = org.mockito.ArgumentCaptor.forClass(ReservaJpaEntity.class);
        verify(repository).save(captor.capture());
        ReservaJpaEntity reserva = captor.getValue();
        assertSame(solicitud, reserva.getSolicitud());
        assertSame(reserva, solicitud.getReserva());
    }
}
