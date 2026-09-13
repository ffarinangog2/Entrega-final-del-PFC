package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.presentation.dto.ErrorResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationEntryPointTest {

    @Test
    void respondeUnauthorizedConErrorJsonCoherente() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException exception = mock(AuthenticationException.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        when(request.getRequestURI()).thenReturn("/api/v1/protegido");
        when(response.getOutputStream()).thenReturn(outputStream);

        entryPoint.commence(request, response, exception);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        var errorCaptor = org.mockito.ArgumentCaptor.forClass(ErrorResponse.class);
        verify(objectMapper).writeValue(any(ServletOutputStream.class), errorCaptor.capture());
        ErrorResponse error = errorCaptor.getValue();
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, error.status());
        assertEquals("Unauthorized", error.error());
        assertEquals("Se requiere un token de acceso válido", error.message());
        assertEquals("/api/v1/protegido", error.path());
    }
}
