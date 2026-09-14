package ec.edu.scli.reservas.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessEventMetricsTest {

    @Test
    void incrementaContadoresConEventosYStatusDeBajaCardinalidad() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessEventMetrics metrics = new BusinessEventMetrics(registry);

        metrics.solicitudCreada();
        metrics.solicitudAprobada();
        metrics.solicitudRechazada();
        metrics.solicitudCancelada();
        metrics.reservaCreada();
        metrics.reservaCancelada();
        metrics.reservaFinalizada();
        metrics.reservaFinalizada();

        assertCounter(registry, "solicitud_creada", 1.0);
        assertCounter(registry, "solicitud_aprobada", 1.0);
        assertCounter(registry, "solicitud_rechazada", 1.0);
        assertCounter(registry, "solicitud_cancelada", 1.0);
        assertCounter(registry, "reserva_creada", 1.0);
        assertCounter(registry, "reserva_cancelada", 1.0);
        assertCounter(registry, "reserva_finalizada", 2.0);
    }

    @Test
    void exportaElNombrePrometheusRequerido() {
        PrometheusMeterRegistry registry =
                new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new BusinessEventMetrics(registry).solicitudCreada();

        String metricLine = registry.scrape().lines()
                .filter(line -> line.startsWith("app_business_events_total{"))
                .findFirst()
                .orElseThrow();

        assertTrue(metricLine.contains("event=\"solicitud_creada\""));
        assertTrue(metricLine.contains("status=\"success\""));
    }

    private void assertCounter(SimpleMeterRegistry registry, String event, double expected) {
        double count = registry.get("app.business.events")
                .tags("event", event, "status", "success")
                .counter()
                .count();
        assertEquals(expected, count);
    }
}
