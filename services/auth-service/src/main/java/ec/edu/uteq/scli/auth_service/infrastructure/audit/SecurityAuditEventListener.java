package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// TODO(auditoria): "acceso_denegado" (403 por RBAC) no se audita todavia.
// Requiere un AccessDeniedHandler real en usuarios/reservas/academico,
// que hoy no existe (solo hay hasAuthority(...) con el 403 por defecto
// de Spring Security). Coordinar con el dueno de RBAC antes de agregarlo.
@Component
public class SecurityAuditEventListener {

    private final AuditLogger auditLogger;

    public SecurityAuditEventListener(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        auditLogger.registrarEvento(
                "login_exitoso",
                event.getAuthentication().getName(),
                obtenerIpCliente(),
                null);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        auditLogger.registrarEvento(
                "login_fallido",
                event.getAuthentication().getName(),
                obtenerIpCliente(),
                event.getException().getClass().getSimpleName());
    }

    private String obtenerIpCliente() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return "desconocida";
        }

        HttpServletRequest request = servletAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
