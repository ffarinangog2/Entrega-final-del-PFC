package ec.edu.scli.reservas.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.stereotype.Component;

/**
 * Conecta el appender de Logback (ver logback-spring.xml) con el OpenTelemetry
 * SDK que ya usan las trazas, para que los logs salgan por OTLP hacia
 * otel-collector -> Loki.
 */
@Component
public class OpenTelemetryLogBridge {

    public OpenTelemetryLogBridge(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
