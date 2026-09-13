package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.domain.service.AccountBlockedException;
import ec.edu.uteq.scli.auth_service.domain.service.AccountDisabledException;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidCredentialsException;
import ec.edu.uteq.scli.auth_service.domain.service.UsuarioServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeaCredencialesInvalidas() {
        assertStatus(HttpStatus.UNAUTHORIZED, handler.handleInvalidCredentials(
                new InvalidCredentialsException(), request));
    }

    @Test
    void mapeaCuentaBloqueada() {
        assertStatus(HttpStatus.LOCKED, handler.handleAccountBlocked(
                new AccountBlockedException(), request));
    }

    @Test
    void mapeaCuentaDeshabilitada() {
        assertStatus(HttpStatus.FORBIDDEN, handler.handleAccountDisabled(
                new AccountDisabledException(), request));
    }

    @Test
    void mapeaUsuariosNoDisponible() {
        when(request.getRequestURI()).thenReturn("/auth");

        ResponseEntity<?> response = handler.handleUsuarioServiceUnavailable(
                new UsuarioServiceUnavailableException("fallo", new RuntimeException()), request);

        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, response);
        assertEquals("/auth", ((ec.edu.uteq.scli.auth_service.presentation.dto.ErrorResponse)
                response.getBody()).path());
    }

    private static void assertStatus(HttpStatus status, ResponseEntity<?> response) {
        assertEquals(status, response.getStatusCode());
    }
}
