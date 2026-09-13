package ec.edu.uteq.scli.auth_service.infrastructure.metrics;

import ec.edu.uteq.scli.auth_service.domain.repository.RefreshSessionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class AuthenticationMetrics {
    private final Counter authenticationSuccess;
    private final Counter authenticationFailure;
    private final Counter tokenRefresh;
    private final Counter logout;

    public AuthenticationMetrics(MeterRegistry registry, RefreshSessionRepository sessions, Clock clock) {
        authenticationSuccess = Counter.builder("app.authentication.success").register(registry);
        authenticationFailure = Counter.builder("app.authentication.failure").register(registry);
        tokenRefresh = Counter.builder("app.token.refresh").register(registry);
        logout = Counter.builder("app.logout").register(registry);
        Gauge.builder("app.active.sessions", sessions,
                        repository -> repository.contarActivas(OffsetDateTime.now(clock)))
                .description("Sesiones refresh no revocadas y no expiradas en la base de datos")
                .register(registry);
    }

    public void authenticationSuccess() { authenticationSuccess.increment(); }
    public void authenticationFailure() { authenticationFailure.increment(); }
    public void tokenRefresh() { tokenRefresh.increment(); }
    public void logout() { logout.increment(); }
}
