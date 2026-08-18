package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.*;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.presentation.dto.request.*;
import ec.edu.scli.reservas.presentation.dto.response.*;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ControllerDelegationTest {
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID recursoId = UUID.randomUUID();
    private final Principal principal = () -> usuarioId.toString();

    @Test
    void reservaControllerDelegaTodasLasOperaciones() {
        ReservaService service = mock(ReservaService.class);
        ReservaResponse reserva = mock(ReservaResponse.class);
        PaginaResponse<ReservaResponse> pagina = new PaginaResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(service.listar(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.buscarPorId(any())).thenReturn(reserva);
        when(service.listarPorLaboratorio(any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.listarPorResponsable(any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.obtenerCalendario(any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.cancelar(any(), any(), any())).thenReturn(reserva);
        when(service.iniciar(any(), any())).thenReturn(reserva);
        when(service.finalizar(any(), any())).thenReturn(reserva);
        when(service.marcarNoAsistida(any(), any())).thenReturn(reserva);
        ReservaController controller = new ReservaController(service);
        LocalDate hoy = LocalDate.now();

        assertEquals(200, controller.listar(null, null, null, null, null, 0, 10).getStatusCode().value());
        assertSame(reserva, controller.buscarPorId(recursoId).getBody());
        assertSame(pagina, controller.listarPorLaboratorio(recursoId, 0, 10).getBody());
        assertSame(pagina, controller.listarPorResponsable(recursoId, 0, 10).getBody());
        assertSame(pagina, controller.obtenerCalendario(recursoId, hoy, hoy, 0, 10).getBody());
        assertSame(reserva, controller.cancelar(recursoId, new CancelarReservaRequest("x"), principal).getBody());
        assertSame(reserva, controller.iniciar(recursoId, principal).getBody());
        assertSame(reserva, controller.finalizar(recursoId, principal).getBody());
        assertSame(reserva, controller.marcarNoAsistida(recursoId, principal).getBody());
        assertThrows(IllegalStateException.class, () -> controller.iniciar(recursoId, null));
    }

    @Test
    void solicitudControllerDelegaTodasLasOperacionesYUsaUsuarioAutenticado() {
        SolicitudReservaService service = mock(SolicitudReservaService.class);
        SolicitudReservaResponse solicitud = mock(SolicitudReservaResponse.class);
        ReservaResponse reserva = mock(ReservaResponse.class);
        when(solicitud.id()).thenReturn(recursoId);
        PaginaResponse<SolicitudReservaResponse> pagina = new PaginaResponse<>(List.of(), 0, 10, 0, 0, true, true);
        PaginaResponse<HistorialSolicitudResponse> historial = new PaginaResponse<>(List.of(), 0, 10, 0, 0, true, true);
        when(service.crear(any(), anyString(), any())).thenReturn(solicitud);
        when(service.listar(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.buscarPorId(any())).thenReturn(solicitud);
        when(service.listarPorSolicitante(any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.listarPorEstado(any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.actualizar(any(), any(), any())).thenReturn(solicitud);
        when(service.ponerEnRevision(any(), any())).thenReturn(solicitud);
        when(service.aprobar(any(), any(), anyString(), any())).thenReturn(reserva);
        when(service.rechazar(any(), any(), any())).thenReturn(solicitud);
        when(service.cancelar(any(), any(), any())).thenReturn(solicitud);
        when(service.obtenerHistorial(any(), anyInt(), anyInt())).thenReturn(historial);
        SolicitudReservaController controller = new SolicitudReservaController(service);
        CrearSolicitudReservaRequest crear = crearRequest();
        ActualizarSolicitudReservaRequest actualizar = new ActualizarSolicitudReservaRequest(
                crear.docenteId(), crear.laboratorioId(), crear.materiaId(), crear.periodoLectivoId(),
                crear.fechaReserva(), crear.horaInicio(), crear.horaFin(), 10, "x", null);

        assertEquals(201, controller.crear(crear, "clave", principal).getStatusCode().value());
        verify(service).crear(argThat(r -> usuarioId.equals(r.solicitanteId())), eq("clave"), eq(usuarioId));
        assertSame(pagina, controller.listar(null, null, null, null, 0, 10).getBody());
        assertSame(solicitud, controller.buscarPorId(recursoId).getBody());
        assertSame(pagina, controller.listarPorSolicitante(usuarioId, 0, 10).getBody());
        assertSame(pagina, controller.listarPorEstado(EstadoSolicitud.PENDIENTE, 0, 10).getBody());
        assertSame(solicitud, controller.actualizar(recursoId, actualizar, principal).getBody());
        assertSame(solicitud, controller.ponerEnRevision(recursoId, principal).getBody());
        assertSame(reserva, controller.aprobar(recursoId, new AprobarSolicitudRequest(usuarioId, null), "k", principal).getBody());
        assertSame(solicitud, controller.rechazar(recursoId, new RechazarSolicitudRequest("x"), principal).getBody());
        assertSame(solicitud, controller.cancelar(recursoId, new CancelarSolicitudRequest("x"), principal).getBody());
        assertSame(historial, controller.obtenerHistorial(recursoId, 0, 10).getBody());
    }

    @Test
    void agendaControllerDelegaYConstruyeRespuestasHttp() {
        AgendaService service = mock(AgendaService.class);
        PaginaResponse<AgendaItemResponse> pagina = new PaginaResponse<>(List.of(), 0, 10, 0, 0, true, true);
        BloqueoAgendaResponse bloqueo = mock(BloqueoAgendaResponse.class);
        when(bloqueo.id()).thenReturn(recursoId);
        when(service.listar(any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.listarPorLaboratorio(any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);
        when(service.crearBloqueo(any(), any())).thenReturn(bloqueo);
        AgendaController controller = new AgendaController(service);
        LocalDate hoy = LocalDate.now();

        assertSame(pagina, controller.listar(null, hoy, hoy, 0, 10).getBody());
        assertSame(pagina, controller.listarPorLaboratorio(recursoId, hoy, hoy, 0, 10).getBody());
        assertEquals(201, controller.crearBloqueo(
                new CrearBloqueoAgendaRequest(recursoId, hoy, LocalTime.of(8, 0), LocalTime.of(9, 0), "x"),
                principal).getStatusCode().value());
        assertEquals(204, controller.eliminarBloqueo(recursoId, principal).getStatusCode().value());
    }

    @Test
    void disponibilidadControllerValidaHorarioYDelega() {
        DisponibilidadService service = mock(DisponibilidadService.class);
        DisponibilidadResponse respuesta = mock(DisponibilidadResponse.class);
        when(service.consultar(any(), any(), any(), any())).thenReturn(respuesta);
        DisponibilidadController controller = new DisponibilidadController(service);
        assertSame(respuesta, controller.consultar(
                recursoId, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0)).getBody());
        assertThrows(IllegalArgumentException.class, () -> controller.consultar(
                recursoId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 0)));
    }

    private CrearSolicitudReservaRequest crearRequest() {
        return new CrearSolicitudReservaRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), 10, "clase", null);
    }
}
