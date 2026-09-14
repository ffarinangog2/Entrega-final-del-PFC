package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.application.service.AdminUsuarioService;
import ec.edu.uteq.scli.auth_service.infrastructure.config.SecurityConfig;
import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetailsService;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtAuthenticationEntryPoint;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUsuarioController.class)
@Import(SecurityConfig.class)
class AdminUsuarioSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminUsuarioService service;
    @MockitoBean CustomUserDetailsService userDetails;
    @MockitoBean JwtAuthenticationFilter jwtFilter;
    @MockitoBean JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void pasarFiltroJwtEnPrueba() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
        when(service.listar()).thenReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeAcceder() throws Exception {
        mvc.perform(get("/api/v1/auth/admin/usuarios")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ADMINISTRADOR_PISO")
    void administradorPisoRecibe403() throws Exception { verificar403(); }

    @Test @WithMockUser(roles = "COORDINADOR")
    void coordinadorRecibe403() throws Exception { verificar403(); }

    @Test @WithMockUser(roles = "DOCENTE")
    void docenteRecibe403() throws Exception { verificar403(); }

    @Test @WithMockUser(roles = "ESTUDIANTE")
    void estudianteRecibe403() throws Exception { verificar403(); }

    private void verificar403() throws Exception {
        mvc.perform(get("/api/v1/auth/admin/usuarios")).andExpect(status().isForbidden());
    }
}
