package ec.edu.scli.academico.infrastructure.observability;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Registro centralizado de la métrica http_requests_total.
 *
 * Implementa el patrón Singleton (GoF): garantiza que exista una única
 * instancia responsable de registrar e incrementar el contador de
 * peticiones HTTP, evitando que distintos componentes creen registros
 * duplicados de la misma métrica ante Prometheus.
 */
@Component
public class HttpRequestsMetricsRegistry {

    private static volatile HttpRequestsMetricsRegistry instance;

    private final MeterRegistry meterRegistry;

    public HttpRequestsMetricsRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        instance = this;
    }

    public static HttpRequestsMetricsRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "HttpRequestsMetricsRegistry aun no ha sido inicializado por Spring");
        }
        return instance;
    }

    public void incrementarPeticion(String ruta, String metodo, String status) {
        Counter.builder("http_requests_total")
            .description("Total de peticiones HTTP recibidas por academico-laboratorios-service")
            .tag("route", ruta)
            .tag("method", metodo)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }
}