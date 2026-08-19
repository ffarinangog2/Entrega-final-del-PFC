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
import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.presentation.controller.ReservaController;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        reservaService = mock(ReservaService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .modulesToInstall(new JavaTimeModule())
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

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
