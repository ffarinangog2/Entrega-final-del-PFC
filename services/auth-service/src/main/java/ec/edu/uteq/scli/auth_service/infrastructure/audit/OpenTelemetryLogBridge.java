package ec.edu.uteq.scli.auth_service.infrastructure.audit;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.stereotype.Component;

/**
 * Conecta el appender de Logback (ver logback-spring.xml) con el
 * OpenTelemetry SDK autoconfigurado por spring-boot-starter-opentelemetry,
 * para que los logs (incluida la auditoria) salgan por OTLP hacia
 * otel-collector -> Loki.
 */
@Component
public class OpenTelemetryLogBridge {

    public OpenTelemetryLogBridge(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
