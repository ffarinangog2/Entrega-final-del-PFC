package ec.edu.scli.reservas.experimental;

import ec.edu.scli.reservas.config.SecurityConfig;
import ec.edu.scli.reservas.experimental.application.ExperimentalArbiterService;
import ec.edu.scli.reservas.experimental.domain.ResultadoArbitraje;
import ec.edu.scli.reservas.experimental.presentation.ExperimentalArbiterController;
import ec.edu.scli.reservas.infrastructure.audit.AuditLogger;
import ec.edu.scli.reservas.security.ExperimentalInternalApiKeyFilter;
import ec.edu.scli.reservas.security.JwtAuthenticationFilter;
import ec.edu.scli.reservas.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExperimentalArbiterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        ExperimentalInternalApiKeyFilter.class})
@TestPropertySource(properties = {
        "app.experimental.arbiter.enabled=true",
        "app.internal-api-key=internal-test-key"
})
class ExperimentalArbiterSecurityTest {

    private static final String PATH = "/api/v1/internal/experimentos/arbiter/adjudicar";
    private static final String BODY = """
            {"runId":"run-security","requestId":"req-1",
             "equipmentId":"11111111-1111-1111-1111-111111111111",
             "laboratorioId":"22222222-2222-2222-2222-222222222222",
             "agenteId":"33333333-3333-3333-3333-333333333333",
             "inicio":"2026-09-07T08:00:00Z","fin":"2026-09-07T10:00:00Z",
             "equipmentStatus":"OPERATIVO","equipmentActive":true,
             "equipmentSource":"fixture"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ExperimentalArbiterService service;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditLogger auditLogger;

    @Test
    void sinApiKeyResponde401AntesDelControlador() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void apiKeyIncorrectaResponde401AntesDelControlador() throws Exception {
        mockMvc.perform(post(PATH).header("X-Internal-Api-Key", "incorrecta")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void apiKeyCorrectaAlcanzaElServicioSinBearer() throws Exception {
        when(service.adjudicar(any(), eq("OPERATIVO"), eq(true))).thenReturn(
                new ResultadoArbitraje("run-security", "req-1", "s0", "CONFIRMED",
                        null, 0, null, null, null, Instant.now()));
        mockMvc.perform(post(PATH).header("X-Internal-Api-Key", "internal-test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estrategia").value("s0"));
        verify(service).adjudicar(any(), eq("OPERATIVO"), eq(true));
    }

    @Test
    void endpointProductivoSigueExigiendoJwt() throws Exception {
        mockMvc.perform(get("/api/v1/reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otraRutaInternaNoSeAbreConLaApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/internal/otro")
                        .header("X-Internal-Api-Key", "internal-test-key"))
                .andExpect(status().isUnauthorized());
    }
}
