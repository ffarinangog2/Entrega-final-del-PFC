package ec.edu.scli.reservas.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.scli.reservas.application.service.ReservaService;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import ec.edu.scli.reservas.application.service.IncidenteService;
import ec.edu.scli.reservas.application.service.NotificacionService;
import ec.edu.scli.reservas.presentation.controller.IncidenteController;
import ec.edu.scli.reservas.presentation.controller.NotificacionController;
import ec.edu.scli.reservas.presentation.dto.response.IncidenteResponse;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import ec.edu.scli.reservas.domain.model.*;
import ec.edu.scli.reservas.security.JwtPrincipal;
import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.presentation.controller.ReservaController;
import ec.edu.scli.reservas.presentation.controller.SolicitudReservaController;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.presentation.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerAlternativaRequest;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudReservaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Provider("reservas-solicitudes-service")
@PactFolder("../../tests/contract/target/pacts")
class ReservasProviderPactTest {

    private static final UUID RESERVA_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SOLICITUD_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LABORATORIO_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RESPONSABLE_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private ReservaService reservaService;
    private SolicitudReservaService solicitudService;
    private IncidenteService incidenteService; private NotificacionService notificacionService;

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        reservaService = mock(ReservaService.class);
        solicitudService = mock(SolicitudReservaService.class);
        incidenteService=mock(IncidenteService.class);notificacionService=mock(NotificacionService.class);
        var principal=new JwtPrincipal(UUID.randomUUID(),RESPONSABLE_ID,"pact");
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaService),
                        new SolicitudReservaController(solicitudService),new IncidenteController(incidenteService),new NotificacionController(notificacionService))
                .defaultRequest(get("/").principal(principal))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @State("usuario autenticado puede reportar incidentes") void puedeReportar(){var id=UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(incidenteService.crear(any(),eq(RESPONSABLE_ID))).thenReturn(new IncidenteResponse(id,RESPONSABLE_ID,"Laboratorio 1","Equipo sin red",PrioridadIncidente.ALTA,LocalDate.parse("2026-08-31"),EstadoIncidente.REPORTADO,Instant.parse("2026-08-31T10:00:00Z"),Instant.parse("2026-08-31T10:00:00Z"),0L));}
    @State("usuario autenticado tiene incidentes") void tieneIncidentes(){var id=UUID.fromString("55555555-5555-5555-5555-555555555555");var r=new IncidenteResponse(id,RESPONSABLE_ID,"Laboratorio 1","Equipo sin red",PrioridadIncidente.ALTA,LocalDate.parse("2026-08-31"),EstadoIncidente.REPORTADO,Instant.now(),Instant.now(),0L);
        when(incidenteService.listar(eq(RESPONSABLE_ID),eq(false),anyInt(),anyInt())).thenReturn(new PaginaResponse<>(List.of(r),0,20,1,1,true,true));}
    @State("usuario autenticado registra dispositivo") void registraDispositivo(){when(notificacionService.registrar(any(),any(),eq(RESPONSABLE_ID))).thenReturn(new DispositivoNotificacionResponse(UUID.fromString("55555555-5555-5555-5555-555555555555"),"ANDROID",true,Instant.now(),Instant.now()));}
    @State("usuario autenticado tiene dispositivo registrado") void tieneDispositivoRegistrado(){ }

    @State("existen reservas registradas")
    void existenReservasRegistradas() {
        ReservaResponse reserva = reservaProgramada();
        PaginaResponse<ReservaResponse> pagina = new PaginaResponse<>(
                List.of(reserva), 0, 20, 1, 1, true, true);
        when(reservaService.listar(
                any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pagina);
    }

    @State("existe una reserva con id 11111111-1111-1111-1111-111111111111")
    void existeReservaPorId() {
        when(reservaService.buscarPorId(RESERVA_ID)).thenReturn(reservaProgramada());
    }

    @State("existe una reserva programada cancelable")
    void existeReservaCancelable() {
        ReservaResponse programada = reservaProgramada();
        when(reservaService.cancelar(eq(RESERVA_ID), any(CancelarReservaRequest.class), eq(RESPONSABLE_ID)))
                .thenReturn(new ReservaResponse(programada.id(), programada.solicitudId(),
                        programada.laboratorioId(), programada.responsableId(), programada.fechaReserva(),
                        programada.horaInicio(), programada.horaFin(), EstadoReserva.CANCELADA,
                        programada.codigoReserva(), programada.creadaEn(), programada.actualizadaEn(), 1L));
    }

    @State("existe una solicitud en revision del piso administrado")
    void existeSolicitudParaPropuesta() {
        LocalDate fecha = LocalDate.parse("2026-08-21");
        when(solicitudService.proponerAlternativa(eq(SOLICITUD_ID),
                any(ProponerAlternativaRequest.class), eq(RESPONSABLE_ID)))
                .thenReturn(new SolicitudReservaResponse(
                        SOLICITUD_ID, RESPONSABLE_ID, RESPONSABLE_ID, LABORATORIO_ID,
                        UUID.randomUUID(), UUID.randomUUID(), LocalDate.parse("2026-08-20"),
                        LocalTime.of(8, 0), LocalTime.of(10, 0), 20, "Clase", null,
                        EstadoSolicitud.PROPUESTA, fecha, LocalTime.NOON, LocalTime.of(14, 0),
                        LABORATORIO_ID, "Horario alternativo", null,
                        Instant.parse("2026-08-18T10:00:00Z"),
                        Instant.parse("2026-08-18T10:00:00Z"), 1L));
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verificarInteraccion(PactVerificationContext context) {
        context.verifyInteraction();
    }

    private ReservaResponse reservaProgramada() {
        Instant instant = Instant.parse("2026-08-18T10:00:00Z");
        return new ReservaResponse(
                RESERVA_ID,
                SOLICITUD_ID,
                LABORATORIO_ID,
                RESPONSABLE_ID,
                LocalDate.parse("2026-08-20"),
                LocalTime.parse("08:00:00"),
                LocalTime.parse("10:00:00"),
                EstadoReserva.PROGRAMADA,
                "RES-2026-0001",
                instant,
                instant,
                0L
        );
    }
}
