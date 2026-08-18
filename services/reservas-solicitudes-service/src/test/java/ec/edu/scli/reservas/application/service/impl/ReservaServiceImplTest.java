package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.presentation.dto.request.CancelarReservaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReservaServiceImplTest {
    private ReservaRepositoryPort repository;
    private ReservaServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ReservaRepositoryPort.class);
        when(repository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        service = new ReservaServiceImpl(repository, new ReservaMapper());
    }

    @Test
    void obtieneReservaPorId() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        when(repository.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        assertEquals(reserva.getId(), service.buscarPorId(reserva.getId()).id());
    }

    @Test
    void rechazaIdInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(id));
    }

    @Test
    void listaYMapeaPagina() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        when(repository.buscar(any(), eq(0), eq(10)))
                .thenReturn(new Pagina<>(List.of(reserva), 0, 10, 1, 1, true, true));
        var pagina = service.listar(EstadoReserva.PROGRAMADA, null, null, null, null, 0, 10);
        assertEquals(1, pagina.totalElementos());
        assertEquals(reserva.getId(), pagina.contenido().getFirst().id());
    }

    @Test
    void iniciaReservaProgramada() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        reserva.setFechaReserva(LocalDate.now().minusDays(1));
        prepararActualizacion(reserva);
        assertEquals(EstadoReserva.EN_CURSO, service.iniciar(reserva.getId(), UUID.randomUUID()).estado());
    }

    @Test
    void finalizaReservaEnCurso() {
        Reserva reserva = reserva(EstadoReserva.EN_CURSO);
        prepararActualizacion(reserva);
        assertEquals(EstadoReserva.FINALIZADA, service.finalizar(reserva.getId(), UUID.randomUUID()).estado());
    }

    @Test
    void cancelaReservaProgramada() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        prepararActualizacion(reserva);
        assertEquals(EstadoReserva.CANCELADA,
                service.cancelar(reserva.getId(), new CancelarReservaRequest("motivo"), UUID.randomUUID()).estado());
    }

    @Test
    void marcaNoAsistidaTrasFinalizarFranja() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        reserva.setFechaReserva(LocalDate.now().minusDays(1));
        prepararActualizacion(reserva);
        assertEquals(EstadoReserva.NO_ASISTIDA,
                service.marcarNoAsistida(reserva.getId(), UUID.randomUUID()).estado());
    }

    @Test
    void delegaListadosEspecializados() {
        Pagina<Reserva> vacia = new Pagina<>(List.of(), 0, 10, 0, 0, true, true);
        UUID id = UUID.randomUUID();
        when(repository.buscarPorLaboratorio(id, 0, 10)).thenReturn(vacia);
        when(repository.buscarPorResponsable(id, 0, 10)).thenReturn(vacia);
        when(repository.buscarCalendario(id, LocalDate.now(), LocalDate.now(), 0, 10)).thenReturn(vacia);
        assertTrue(service.listarPorLaboratorio(id, 0, 10).contenido().isEmpty());
        assertTrue(service.listarPorResponsable(id, 0, 10).contenido().isEmpty());
        assertTrue(service.obtenerCalendario(id, LocalDate.now(), LocalDate.now(), 0, 10).contenido().isEmpty());
    }

    private void prepararActualizacion(Reserva reserva) {
        when(repository.buscarPorIdParaActualizar(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private Reserva reserva(EstadoReserva estado) {
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setSolicitudId(UUID.randomUUID());
        reserva.setLaboratorioId(UUID.randomUUID());
        reserva.setResponsableId(UUID.randomUUID());
        reserva.setFechaReserva(LocalDate.now());
        reserva.setHoraInicio(LocalTime.MIN);
        reserva.setHoraFin(LocalTime.MAX);
        reserva.setEstado(estado);
        reserva.setCodigoReserva("RES-1");
        reserva.setVersion(0L);
        return reserva;
    }
}
