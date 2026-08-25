package ec.edu.scli.usuarios.security;

import ec.edu.scli.usuarios.application.usecase.DocenteService;
import ec.edu.scli.usuarios.config.SecurityConfig;
import ec.edu.scli.usuarios.presentation.controller.DocenteController;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import ec.edu.scli.usuarios.infrastructure.observability.HttpRequestsMetricsRegistry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocenteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        HttpRequestsMetricsRegistry.class, UsuariosSecurityIntegrationTest.MetricsTestConfig.class})
@TestPropertySource(properties = {
        "security.jwt.issuer=scli-auth-service",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class UsuariosSecurityIntegrationTest {

    @TestConfiguration
    static class MetricsTestConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocenteService docenteService;

    private UUID perfilId;

    @BeforeEach
    void prepararDocente() {
        perfilId = UUID.randomUUID();
        when(docenteService.obtenerPorPerfilId(perfilId)).thenReturn(
                new DocenteResponse(UUID.randomUUID(), perfilId, "DOC-001",
                        null, null, null, null, true, null, null));
    }

    @Test
    void docentePorPerfilSinJwtRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/perfil/{perfilId}", perfilId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenDelPropioPerfilPuedeResolverDocente() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/perfil/{perfilId}", perfilId)
                        .header("Authorization", "Bearer " + token(
                                perfilId, "access", List.of("ACADEMICO_LEER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilId").value(perfilId.toString()));
    }

    @Test
    void accessTokenSinPermisoNoPuedeConsultarOtroPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/perfil/{perfilId}", perfilId)
                        .header("Authorization", "Bearer " + token(
                                UUID.randomUUID(), "access", List.of("ACADEMICO_LEER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permisoUsuarioLeerPuedeConsultarOtroPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/perfil/{perfilId}", perfilId)
                        .header("Authorization", "Bearer " + token(
                                UUID.randomUUID(), "access", List.of("USUARIO_LEER"))))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenEsRechazado() throws Exception {
        mockMvc.perform(get("/api/v1/docentes/perfil/{perfilId}", perfilId)
                        .header("Authorization", "Bearer " + token(
                                perfilId, "refresh", List.of("USUARIO_LEER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listadoSinPermisoRetorna403() throws Exception {
        mockMvc.perform(get("/api/v1/docentes")
                        .header("Authorization", "Bearer " + token(
                                perfilId, "access", List.of("ACADEMICO_LEER"))))
                .andExpect(status().isForbidden());
    }

    private String token(UUID perfil, String type, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("scli-auth-service")
                .subject(UUID.randomUUID().toString())
                .claim("perfilId", perfil.toString())
                .claim("username", "usuario-prueba")
                .claim("type", type)
                .claim("roles", List.of("DOCENTE"))
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }
}
