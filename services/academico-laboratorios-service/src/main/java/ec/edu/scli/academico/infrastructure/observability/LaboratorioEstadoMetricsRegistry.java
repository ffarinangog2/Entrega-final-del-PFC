package ec.edu.scli.academico.infrastructure.observability;

import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Expone la métrica de dominio laboratorios_por_estado ante Prometheus.
 *
 * A diferencia de HttpRequestsMetricsRegistry (un Counter que se
 * incrementa manualmente en cada petición), esta clase registra un
 * Gauge: Micrometer recalcula el valor por sí solo, consultando la
 * base de datos a través del puerto de dominio, cada vez que
 * Prometheus hace scrape — nunca se actualiza a mano.
 */
@Component
public class LaboratorioEstadoMetricsRegistry {

    private final MeterRegistry meterRegistry;
    private final LaboratorioRepositoryPort laboratorioRepositoryPort;

    public LaboratorioEstadoMetricsRegistry(
            MeterRegistry meterRegistry,
            LaboratorioRepositoryPort laboratorioRepositoryPort
    ) {
        this.meterRegistry = meterRegistry;
        this.laboratorioRepositoryPort = laboratorioRepositoryPort;
    }

    @PostConstruct
    public void registrarGauges() {
        for (EstadoLaboratorio estado : EstadoLaboratorio.values()) {
            meterRegistry.gauge(
                    "laboratorios_por_estado",
                    List.of(Tag.of("estado", estado.name())),
                    laboratorioRepositoryPort,
                    port -> (double) port.contarPorEstado(estado)
            );
        }
    }
}