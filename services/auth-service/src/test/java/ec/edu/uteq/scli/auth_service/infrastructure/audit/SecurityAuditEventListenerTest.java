package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityAuditEventListenerTest {

    @AfterEach
    void limpiarContextoRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loginExitosoSeAuditaConUsuarioEIpDelHeaderForwarded() {
        AuditLogger auditLogger = mock(AuditLogger.class);
        SecurityAuditEventListener listener = new SecurityAuditEventListener(auditLogger);
        simularRequestConIp("203.0.113.9");

        Authentication authentication = new UsernamePasswordAuthenticationToken("jperez", "pw");
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

        verify(auditLogger).registrarEvento(
                eq("login_exitoso"), eq("jperez"), eq("203.0.113.9"), isNull());
    }

    @Test
    void loginFallidoSeAuditaConElNombreDeLaExcepcion() {
        AuditLogger auditLogger = mock(AuditLogger.class);
        SecurityAuditEventListener listener = new SecurityAuditEventListener(auditLogger);
        simularRequestConIp("198.51.100.2");

        Authentication authentication = new UsernamePasswordAuthenticationToken("jperez", "pw");
        BadCredentialsException exception = new BadCredentialsException("credenciales invalidas");
        listener.onAuthenticationFailure(
                new AuthenticationFailureBadCredentialsEvent(authentication, exception));

        verify(auditLogger).registrarEvento(
                eq("login_fallido"), eq("jperez"), eq("198.51.100.2"), eq("BadCredentialsException"));
    }

    @Test
    void sinRequestHttpActivaLaIpSeReportaComoDesconocida() {
        AuditLogger auditLogger = mock(AuditLogger.class);
        SecurityAuditEventListener listener = new SecurityAuditEventListener(auditLogger);

        Authentication authentication = new UsernamePasswordAuthenticationToken("jperez", "pw");
        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(authentication));

        verify(auditLogger).registrarEvento(
                eq("login_exitoso"), eq("jperez"), eq("desconocida"), isNull());
    }

    private void simularRequestConIp(String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
