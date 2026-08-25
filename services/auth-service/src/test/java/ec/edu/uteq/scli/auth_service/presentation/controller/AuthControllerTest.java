package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.application.service.AuthService;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.RefreshTokenRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private LoginResponse loginResponse;

    private final LoginRequest loginRequest = new LoginRequest("admin", "password");
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void delegaLoginYDevuelveRespuestaOk() {
        when(authService.login(loginRequest)).thenReturn(loginResponse);

        var response = controller.login(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertSame(loginResponse, response.getBody());
        verify(authService).login(loginRequest);
    }

    @Test
    void delegaRefreshYDevuelveRespuestaOk() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        when(authService.refrescar("refresh-token")).thenReturn(loginResponse);

        var response = controller.refresh(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(loginResponse, response.getBody());
        verify(authService).refrescar("refresh-token");
    }
}
