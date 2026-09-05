package ec.edu.uteq.scli.auth_service.infrastructure.metrics;

import ec.edu.uteq.scli.auth_service.domain.repository.RefreshSessionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationMetricsTest {
    @Test
    void gaugeConsultaLaBaseYContadoresSeIncrementan() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        when(sessions.contarActivas(any())).thenReturn(7L);
        AuthenticationMetrics metrics = new AuthenticationMetrics(registry, sessions,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        metrics.authenticationSuccess();
        metrics.authenticationFailure();
        metrics.tokenRefresh();
        metrics.logout();

        assertEquals(7.0, registry.get("app.active.sessions").gauge().value());
        assertEquals(1.0, registry.get("app.authentication.success").counter().count());
        assertEquals(1.0, registry.get("app.authentication.failure").counter().count());
        assertEquals(1.0, registry.get("app.token.refresh").counter().count());
        assertEquals(1.0, registry.get("app.logout").counter().count());
    }
}
