package ec.edu.uteq.scli.auth_service.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private CustomUserDetails userDetails;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        response = new MockHttpServletResponse();
    }

    @Test
    void continuaSinAuthorization() throws Exception {
        MockHttpServletRequest request = request("/api/v1/private");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).esTokenValido(any());
    }

    @Test
    void autenticaBearerValido() throws Exception {
        MockHttpServletRequest request = request("/api/v1/private");
        request.addHeader("Authorization", "Bearer token");
        UUID usuarioId = UUID.randomUUID();
        when(jwtService.esTokenValido("token")).thenReturn(true);
        when(jwtService.extraerUsuarioId("token")).thenReturn(usuarioId);
        when(userDetailsService.loadUserById(usuarioId)).thenReturn(userDetails);

        filter.doFilter(request, response, filterChain);

        assertTrue(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().isAuthenticated());
        verify(filterChain).doFilter(request, response);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void ignoraBearerVacio() throws Exception {
        MockHttpServletRequest request = request("/api/v1/private");
        request.addHeader("Authorization", "Bearer ");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).esTokenValido(any());
    }

    @Test
    void limpiaContextoCuandoTokenInvalido() throws Exception {
        MockHttpServletRequest request = request("/api/v1/private");
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.esTokenValido("token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertFalse(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication() != null);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noFiltraRutasPublicas() {
        assertTrue(filter.shouldNotFilter(request("/api/v1/auth/login")));
        assertTrue(filter.shouldNotFilter(request("/api/v1/auth/refresh")));
        assertTrue(filter.shouldNotFilter(request("/actuator/health")));
        assertTrue(filter.shouldNotFilter(request("/swagger-ui/index.html")));
        assertFalse(filter.shouldNotFilter(request("/api/v1/private")));
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return request;
    }
}
