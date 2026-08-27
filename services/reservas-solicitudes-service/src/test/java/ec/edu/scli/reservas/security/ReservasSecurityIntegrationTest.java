package ec.edu.scli.reservas.security;

import ec.edu.scli.reservas.application.service.DisponibilidadService;
import ec.edu.scli.reservas.config.SecurityConfig;
import ec.edu.scli.reservas.infrastructure.audit.AuditLogger;
import ec.edu.scli.reservas.presentation.controller.DisponibilidadController;
import ec.edu.scli.reservas.presentation.controller.SolicitudReservaController;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;

@WebMvcTest({DisponibilidadController.class, SolicitudReservaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "security.jwt.issuer=scli-auth-service",
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class ReservasSecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private DisponibilidadService disponibilidadService;
    @MockitoBean private SolicitudReservaService solicitudService;
    @MockitoBean private AuditLogger auditLogger;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(consulta()).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenConPermisoEsAceptado() throws Exception {
        mockMvc.perform(consulta().header("Authorization", "Bearer " + token("access", List.of("LABORATORIO_LEER"))))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenEsRechazadoCon401() throws Exception {
        mockMvc.perform(consulta().header("Authorization", "Bearer " + token("refresh", List.of("LABORATORIO_LEER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenSinPermisoResponde403() throws Exception {
        mockMvc.perform(consulta().header("Authorization", "Bearer " + token("access", List.of("RESERVA_LEER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessTokenSinPermisoAuditaAccesoDenegado() throws Exception {
        mockMvc.perform(consulta().header("Authorization", "Bearer " + token("access", List.of("RESERVA_LEER"))))
                .andExpect(status().isForbidden());

        verify(auditLogger).registrarEvento(
                eq("acceso_denegado"), any(), any(), org.mockito.ArgumentMatchers.contains("/api/v1/disponibilidad/laboratorios/"));
    }

    @Test
    void docenteConPermisoActualPuedeAceptarPropuesta() throws Exception {
        UUID perfilId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/solicitudes/{id}/propuesta/aceptar", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(
                                "access", "DOCENTE", perfilId, List.of("SOLICITUD_CREAR")))
                        .contentType("application/json").content("{\"comentario\":\"Acepto\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void docenteAjenoEsRechazadoPorOwnershipAunqueTengaPermisoHttp() throws Exception {
        UUID perfilAjeno = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        when(solicitudService.aceptarPropuesta(eq(solicitudId), any(), eq(perfilAjeno)))
                .thenThrow(new AccessDeniedException("La solicitud pertenece a otro usuario"));
        mockMvc.perform(post("/api/v1/solicitudes/{id}/propuesta/aceptar", solicitudId)
                        .header("Authorization", "Bearer " + token(
                                "access", "DOCENTE", perfilAjeno, List.of("SOLICITUD_CREAR")))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorPisoNoPuedeAceptarSinPermisoDeCreacion() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes/{id}/propuesta/aceptar", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token(
                                "access", "ADMINISTRADOR_PISO", UUID.randomUUID(),
                                List.of("SOLICITUD_APROBAR")))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder consulta() {
        return get("/api/v1/disponibilidad/laboratorios/{id}", UUID.randomUUID())
                .param("fecha", "2030-01-01").param("horaInicio", "08:00").param("horaFin", "09:00");
    }

    private String token(String type, List<String> permissions) {
        return token(type, "DOCENTE", UUID.randomUUID(), permissions);
    }

    private String token(String type, String role, UUID perfilId, List<String> permissions) {
        return Jwts.builder().issuer("scli-auth-service")
                .subject(UUID.randomUUID().toString())
                .claim("perfilId", perfilId.toString())
                .claim("username", "usuario@scli.edu.ec")
                .claim("type", type).claim("roles", List.of(role))
                .claim("permissions", permissions)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("0123456789abcdef0123456789abcdef"
                        .getBytes(StandardCharsets.UTF_8))).compact();
    }
}
