package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.SolicitudReservaRepositoryPort;
import ec.edu.scli.reservas.infrastructure.audit.AuditLogger;
import ec.edu.scli.reservas.mapper.ReservaMapper;
import ec.edu.scli.reservas.observability.BusinessEventMetrics;
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
    private BusinessEventMetrics metrics;
    private PoliticaAmbitoLaboratorio politica;
    private SolicitudReservaRepositoryPort solicitudes;
    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        repository = mock(ReservaRepositoryPort.class);
        metrics = mock(BusinessEventMetrics.class);
        politica = mock(PoliticaAmbitoLaboratorio.class);
        solicitudes = mock(SolicitudReservaRepositoryPort.class);
        auditLogger = mock(AuditLogger.class);
        when(politica.actor()).thenReturn(new ActorAutenticado(
                UUID.randomUUID(), java.util.Set.of("ROLE_ADMINISTRADOR")));
        when(repository.guardar(any())).thenAnswer(i -> i.getArgument(0));
        service = new ReservaServiceImpl(
                repository, new ReservaMapper(), metrics, politica, solicitudes, auditLogger);
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
        verify(metrics).reservaFinalizada();
    }

    @Test
    void cancelaReservaProgramada() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        prepararActualizacion(reserva);
        assertEquals(EstadoReserva.CANCELADA,
                service.cancelar(reserva.getId(), new CancelarReservaRequest("motivo"), UUID.randomUUID()).estado());
        verify(metrics).reservaCancelada();
    }

    @Test
    void cancelaReservaAuditaReservaCancelada() {
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        prepararActualizacion(reserva);
        service.cancelar(reserva.getId(), new CancelarReservaRequest("motivo"), UUID.randomUUID());
        verify(auditLogger).registrarEvento(
                eq("reserva_cancelada"), any(), any(), contains("id=" + reserva.getId()));
    }

    @Test
    void docenteCancelaSuPropiaReserva() {
        UUID docente = UUID.randomUUID();
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        reserva.setResponsableId(docente);
        prepararActualizacion(reserva);
        when(politica.actor()).thenReturn(new ActorAutenticado(docente, java.util.Set.of("ROLE_DOCENTE")));
        assertEquals(EstadoReserva.CANCELADA,
                service.cancelar(reserva.getId(), new CancelarReservaRequest("motivo"), docente).estado());
        verify(politica, never()).validarGestion(any());
    }

    @Test
    void docenteNoPuedeCancelarReservaAjena() {
        UUID docente = UUID.randomUUID();
        Reserva reserva = reserva(EstadoReserva.PROGRAMADA);
        prepararActualizacion(reserva);
        when(politica.actor()).thenReturn(new ActorAutenticado(docente, java.util.Set.of("ROLE_DOCENTE")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.cancelar(reserva.getId(), new CancelarReservaRequest("motivo"), docente));
        verify(repository, never()).guardar(any());
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
        when(repository.buscar(any(), eq(0), eq(10))).thenReturn(vacia);
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
