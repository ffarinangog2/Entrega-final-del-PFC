package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.application.service.AuthService;
import ec.edu.uteq.scli.auth_service.application.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.RefreshTokenRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.ForgotPasswordRequest;
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
    @Mock private PasswordRecoveryService passwordRecoveryService;
    @Mock private HttpServletRequest httpRequest;

    private final LoginRequest loginRequest = new LoginRequest("admin", "password");
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, passwordRecoveryService);
    }

    @Test
    void delegaLoginYDevuelveRespuestaOk() {
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.login(org.mockito.ArgumentMatchers.eq(loginRequest), org.mockito.ArgumentMatchers.any())).thenReturn(loginResponse);

        var response = controller.login(loginRequest, httpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertSame(loginResponse, response.getBody());
        verify(authService).login(org.mockito.ArgumentMatchers.eq(loginRequest), org.mockito.ArgumentMatchers.any());
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

    @Test
    void delegaLogoutYDevuelveNoContent() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        var response = controller.logout(request);

        assertEquals(204, response.getStatusCode().value());
        verify(authService).logout("refresh-token");
    }

    @Test
    void forgotPasswordSiempreDevuelveMensajeNeutro() {
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        var response=controller.forgotPassword(new ForgotPasswordRequest("cualquier-identificador"),httpRequest);
        assertEquals(200,response.getStatusCode().value());
        assertEquals(PasswordRecoveryService.NEUTRAL_MESSAGE,response.getBody().message());
    }
}
