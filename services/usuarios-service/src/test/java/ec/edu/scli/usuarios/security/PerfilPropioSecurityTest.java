package ec.edu.scli.usuarios.security;

import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.config.SecurityConfig;
import ec.edu.scli.usuarios.infrastructure.audit.AuditLogger;
import ec.edu.scli.usuarios.infrastructure.observability.HttpRequestsMetricsRegistry;
import ec.edu.scli.usuarios.presentation.controller.PerfilController;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PerfilController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        HttpRequestsMetricsRegistry.class, PerfilPropioSecurityTest.Config.class})
@TestPropertySource(properties = {
        "security.jwt.issuer=scli-auth-service",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
class PerfilPropioSecurityTest {
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    @TestConfiguration static class Config { @Bean SimpleMeterRegistry meterRegistry() { return new SimpleMeterRegistry(); } }
    @Autowired MockMvc mvc;
    @MockitoBean PerfilService perfiles;
    @MockitoBean AuditLogger auditLogger;

    @Test void meSinJwtRetorna401() throws Exception {
        mvc.perform(get("/api/v1/perfiles/me")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test void usuarioSinPermisoGlobalConsultaSuPerfil() throws Exception {
        UUID id = UUID.randomUUID();
        when(perfiles.obtenerPorId(id)).thenReturn(perfil(id));
        mvc.perform(get("/api/v1/perfiles/me").header("Authorization", "Bearer " + token(id, List.of())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        verify(perfiles).obtenerPorId(id);
    }

    @Test void usuarioSinPermisoGlobalActualizaSoloSuPerfil() throws Exception {
        UUID id = UUID.randomUUID();
        when(perfiles.actualizarPropio(any(), any())).thenReturn(perfil(id));
        mvc.perform(patch("/api/v1/perfiles/me")
                        .header("Authorization", "Bearer " + token(id, List.of()))
                        .contentType("application/json")
                        .content("{\"emailPersonal\":\"ana@example.com\",\"telefono\":\"0999999999\",\"direccion\":\"X\",\"fotoUrl\":null}"))
                .andExpect(status().isOk());
        verify(perfiles).actualizarPropio(org.mockito.ArgumentMatchers.eq(id), any());
    }

    @Test void usuarioNoPuedeEditarPerfilAjenoPorCrudAdministrativo() throws Exception {
        UUID propio = UUID.randomUUID();
        mvc.perform(put("/api/v1/perfiles/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(propio, List.of()))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verify(perfiles, never()).actualizar(any(), any());
    }

    private PerfilResponse perfil(UUID id) {
        return new PerfilResponse(id, "0102030405", "Ana", "Torres", "ana@uteq.edu.ec",
                null, null, null, null, null, true, null, null);
    }

    private String token(UUID perfil, List<String> permisos) {
        Instant now = Instant.now();
        return Jwts.builder().issuer("scli-auth-service").subject(UUID.randomUUID().toString())
                .claim("perfilId", perfil.toString()).claim("username", "ana").claim("type", "access")
                .claim("roles", List.of("ESTUDIANTE")).claim("permissions", permisos)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))).compact();
    }
}
