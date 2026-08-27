package ec.edu.scli.academico.security;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.config.SecurityConfig;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.audit.AuditLogger;
import ec.edu.scli.academico.infrastructure.observability.HttpMetricsFilter;
import ec.edu.scli.academico.presentation.controller.InternalController;
import ec.edu.scli.academico.presentation.controller.PeriodoLectivoController;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {PeriodoLectivoController.class, InternalController.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = HttpMetricsFilter.class))
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "security.jwt.issuer=scli-auth-service",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.internal-api-key=internal-test-key"
})
class AcademicoSecurityIntegrationTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PeriodoLectivoService periodoLectivoService;

    @MockitoBean
    private LaboratorioService laboratorioService;

    @MockitoBean
    private MateriaService materiaService;

    @MockitoBean
    private AuditLogger auditLogger;

    @Test
    void lecturaAcademicaSinJwtDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/periodos-lectivos/actual"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void academicoLeerPermiteLectura() throws Exception {
        when(periodoLectivoService.obtenerActual()).thenReturn(periodo());
        mockMvc.perform(get("/api/v1/periodos-lectivos/actual")
                        .header("Authorization", bearer(List.of("DOCENTE"), List.of("ACADEMICO_LEER"))))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenNoSeAceptaComoBearerDeLectura() throws Exception {
        mockMvc.perform(get("/api/v1/periodos-lectivos/actual")
                        .header("Authorization", bearer(
                                List.of("DOCENTE"), List.of("ACADEMICO_LEER"), "refresh")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalAceptaClaveCorrectaSinJwt() throws Exception {
        UUID id = UUID.randomUUID();
        when(materiaService.verificarExistencia(id)).thenReturn(new ExisteResponse(id, true));
        mockMvc.perform(get("/api/v1/internal/materias/{id}/exists", id)
                        .header("X-Internal-Api-Key", "internal-test-key"))
                .andExpect(status().isOk());
    }

    @Test
    void internalRechazaClaveAusenteOIncorrecta() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/internal/materias/{id}/exists", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/internal/materias/{id}/exists", id)
                        .header("X-Internal-Api-Key", "incorrecta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void planificacionGestionarPermiteCrearHorarioAcademicoBase() throws Exception {
        when(periodoLectivoService.crear(any())).thenReturn(periodo());
        mockMvc.perform(post("/api/v1/periodos-lectivos")
                        .header("Authorization", bearer(List.of("COORDINADOR"),
                                List.of("ACADEMICO_LEER", "PLANIFICACION_GESTIONAR")))
                        .contentType("application/json")
                        .content(periodoJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void docenteNoObtieneEscrituraAcademicaPorAccidente() throws Exception {
        mockMvc.perform(post("/api/v1/periodos-lectivos")
                        .header("Authorization", bearer(List.of("DOCENTE"), List.of("ACADEMICO_LEER")))
                        .contentType("application/json")
                        .content(periodoJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void docenteSinPermisoAuditaAccesoDenegado() throws Exception {
        mockMvc.perform(post("/api/v1/periodos-lectivos")
                        .header("Authorization", bearer(List.of("DOCENTE"), List.of("ACADEMICO_LEER")))
                        .contentType("application/json")
                        .content(periodoJson()))
                .andExpect(status().isForbidden());

        verify(auditLogger).registrarEvento(
                eq("acceso_denegado"), any(), any(), eq("POST /api/v1/periodos-lectivos"));
    }

    private String bearer(List<String> roles, List<String> permissions) {
        return bearer(roles, permissions, "access");
    }

    private String bearer(List<String> roles, List<String> permissions, String type) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("scli-auth-service")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .claim("perfilId", UUID.randomUUID().toString())
                .claim("username", "usuario.demo")
                .claim("type", type)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        return "Bearer " + token;
    }

    private PeriodoLectivoResponse periodo() {
        return new PeriodoLectivoResponse(UUID.randomUUID(), "DEMO-2026", "Periodo DEMO",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoPeriodo.ACTIVO, null, null);
    }

    private String periodoJson() {
        return """
                {"codigo":"DEMO-2026","nombre":"Periodo DEMO",\
                "fechaInicio":"2026-01-01","fechaFin":"2026-12-31","estado":"ACTIVO"}
                """;
    }
}
