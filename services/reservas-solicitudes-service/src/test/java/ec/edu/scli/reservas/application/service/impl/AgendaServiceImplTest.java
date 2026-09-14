package ec.edu.scli.reservas.application.service.impl;

import ec.edu.scli.reservas.client.AcademicoLaboratoriosClient;
import ec.edu.scli.reservas.client.dto.LaboratorioExternoResponse;
import ec.edu.scli.reservas.application.service.PoliticaAmbitoLaboratorio;
import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.Reserva;
import ec.edu.scli.reservas.domain.port.out.ReservaRepositoryPort;
import ec.edu.scli.reservas.domain.port.out.BloqueoAgendaRepositoryPort;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import ec.edu.scli.reservas.mapper.BloqueoAgendaMapper;
import ec.edu.scli.reservas.presentation.dto.request.CrearBloqueoAgendaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgendaServiceImplTest {
    private ReservaRepositoryPort reservas;
    private BloqueoAgendaRepositoryPort bloqueos;
    private AcademicoLaboratoriosClient academico;
    private TransactionTemplate transactions;
    private PoliticaAmbitoLaboratorio politica;
    private AgendaServiceImpl service;
    private final UUID laboratorioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reservas = mock(ReservaRepositoryPort.class);
        bloqueos = mock(BloqueoAgendaRepositoryPort.class);
        academico = mock(AcademicoLaboratoriosClient.class);
        transactions = mock(TransactionTemplate.class);
        politica = mock(PoliticaAmbitoLaboratorio.class);
        service = new AgendaServiceImpl(
                reservas, bloqueos, new BloqueoAgendaMapper(), academico, transactions, politica);
        when(academico.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(
                        laboratorioId, UUID.randomUUID(), true, true, "ACTIVO", 30));
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        when(bloqueos.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void listaReservasYBloqueosOrdenadosYPaginados() {
        LocalDate fecha = LocalDate.now();
        Reserva reserva = reserva(fecha, LocalTime.of(10, 0));
        BloqueoAgenda bloqueo = bloqueo(fecha, LocalTime.of(8, 0), true);
        when(reservas.buscarParaAgenda(laboratorioId, fecha, fecha)).thenReturn(List.of(reserva));
        when(bloqueos.buscarActivos(any(), any(), any())).thenReturn(List.of(bloqueo));

        var pagina = service.listar(laboratorioId, fecha, fecha, 0, 10);

        assertEquals(2, pagina.totalElementos());
        assertEquals("BLOQUEO", pagina.contenido().getFirst().tipo());
        assertEquals("RESERVA", pagina.contenido().getLast().tipo());
    }

    @Test
    void listaPorLaboratorioDelegaYValidaId() {
        LocalDate fecha = LocalDate.now();
        when(reservas.buscarParaAgenda(laboratorioId, fecha, fecha)).thenReturn(List.of());
        when(bloqueos.buscarActivos(any(), any(), any())).thenReturn(List.of());
        assertTrue(service.listarPorLaboratorio(laboratorioId, fecha, fecha, 0, 10).contenido().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.listarPorLaboratorio(null, fecha, fecha, 0, 10));
    }

    @Test
    void validaPaginacionYRango() {
        LocalDate hoy = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () -> service.listar(null, hoy, hoy, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.listar(null, hoy, hoy, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.listar(null, hoy.plusDays(1), hoy, 0, 10));
    }

    @Test
    void creaBloqueoSinConflictos() {
        CrearBloqueoAgendaRequest request = request();
        var respuesta = service.crearBloqueo(request, usuarioId);
        assertEquals(laboratorioId, respuesta.laboratorioId());
        assertTrue(respuesta.activo());
        verify(bloqueos).guardar(argThat(b -> usuarioId.equals(b.getCreadoPor())));
    }

    @Test
    void rechazaBloqueoConflictivo() {
        when(bloqueos.contarActivosConflictivos(any(), any(), any(), any())).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.crearBloqueo(request(), usuarioId));
        verify(bloqueos, never()).guardar(any());
    }

    @Test
    void rechazaReservaConflictivaAlCrearBloqueo() {
        when(reservas.contarConflictosActivos(any(), any(), any(), any())).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.crearBloqueo(request(), usuarioId));
        verify(bloqueos, never()).guardar(any());
    }

    @Test
    void validaSolicitudUsuarioHorarioYLaboratorio() {
        assertThrows(IllegalArgumentException.class, () -> service.crearBloqueo(null, usuarioId));
        assertThrows(IllegalArgumentException.class, () -> service.crearBloqueo(request(), null));
        var horarioInvalido = new CrearBloqueoAgendaRequest(
                laboratorioId, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(9, 0), "x");
        assertThrows(IllegalArgumentException.class, () -> service.crearBloqueo(horarioInvalido, usuarioId));
        when(academico.obtenerLaboratorio(laboratorioId))
                .thenReturn(new LaboratorioExternoResponse(
                        laboratorioId, null, false, false, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.crearBloqueo(request(), usuarioId));
    }

    @Test
    void eliminaBloqueoActivoYRespetaInactivo() {
        BloqueoAgenda activo = bloqueo(LocalDate.now(), LocalTime.of(8, 0), true);
        when(bloqueos.buscarPorId(activo.getId())).thenReturn(Optional.of(activo));
        service.eliminarBloqueo(activo.getId(), usuarioId);
        assertFalse(activo.getActivo());
        verify(bloqueos).guardar(activo);

        BloqueoAgenda inactivo = bloqueo(LocalDate.now(), LocalTime.of(9, 0), false);
        when(bloqueos.buscarPorId(inactivo.getId())).thenReturn(Optional.of(inactivo));
        service.eliminarBloqueo(inactivo.getId(), usuarioId);
        verify(bloqueos, never()).guardar(inactivo);
    }

    private CrearBloqueoAgendaRequest request() {
        return new CrearBloqueoAgendaRequest(
                laboratorioId, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(10, 0), "mantenimiento");
    }

    private Reserva reserva(LocalDate fecha, LocalTime inicio) {
        Reserva r = new Reserva(); r.setId(UUID.randomUUID()); r.setLaboratorioId(laboratorioId);
        r.setFechaReserva(fecha); r.setHoraInicio(inicio); r.setHoraFin(inicio.plusHours(1));
        r.setEstado(EstadoReserva.PROGRAMADA); r.setCodigoReserva("RES-1"); return r;
    }

    private BloqueoAgenda bloqueo(LocalDate fecha, LocalTime inicio, boolean activo) {
        BloqueoAgenda b = BeanUtils.instantiateClass(BloqueoAgenda.class); b.setId(UUID.randomUUID()); b.setLaboratorioId(laboratorioId);
        b.setFecha(fecha); b.setHoraInicio(inicio); b.setHoraFin(inicio.plusHours(1));
        b.setMotivo("mantenimiento"); b.setCreadoPor(usuarioId); b.setActivo(activo); b.setVersion(0L); return b;
    }
}
