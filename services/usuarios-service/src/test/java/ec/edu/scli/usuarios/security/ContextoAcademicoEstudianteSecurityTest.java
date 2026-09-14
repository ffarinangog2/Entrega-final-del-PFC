package ec.edu.scli.usuarios.security;

import ec.edu.scli.usuarios.application.service.ContextoAcademicoEstudianteService;
import ec.edu.scli.usuarios.config.SecurityConfig;
import ec.edu.scli.usuarios.infrastructure.audit.AuditLogger;
import ec.edu.scli.usuarios.infrastructure.observability.HttpRequestsMetricsRegistry;
import ec.edu.scli.usuarios.presentation.controller.ContextoAcademicoEstudianteController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextoAcademicoEstudianteController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        HttpRequestsMetricsRegistry.class, ContextoAcademicoEstudianteSecurityTest.Config.class})
@TestPropertySource(properties = {
        "security.jwt.issuer=scli-auth-service",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
class ContextoAcademicoEstudianteSecurityTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @TestConfiguration
    static class Config {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContextoAcademicoEstudianteService service;

    @MockitoBean
    private AuditLogger auditLogger;

    @Test
    void endpointMasivoSinJwtRetorna401() throws Exception {
        mvc.perform(get("/api/v1/estudiantes/contextos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void endpointMasivoParaEstudianteSinUsuarioLeerRetorna403() throws Exception {
        // Estudiante con sus permisos estándar (INCIDENTE_LEER, etc.) pero sin USUARIO_LEER
        String tokenEstudiante = token(UUID.randomUUID(), List.of("ESTUDIANTE"), List.of("INCIDENTE_LEER", "INCIDENTE_CREAR"));

        mvc.perform(get("/api/v1/estudiantes/contextos")
                        .header("Authorization", "Bearer " + tokenEstudiante))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void endpointMasivoConPermisoUsuarioLeerRetorna200() throws Exception {
        when(service.listarContextosMasivos(any())).thenReturn(List.of());

        // Token con permiso administrativo USUARIO_LEER
        String tokenAdmin = token(UUID.randomUUID(), List.of("ADMINISTRADOR"), List.of("USUARIO_LEER"));

        mvc.perform(get("/api/v1/estudiantes/contextos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    private String token(UUID perfil, List<String> roles, List<String> permisos) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("scli-auth-service").subject(UUID.randomUUID().toString())
                .claim("perfilId", perfil.toString()).claim("username", "usuario.test").claim("type", "access")
                .claim("roles", roles).claim("permissions", permisos)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
    }
}
