package ec.edu.scli.reservas.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Registra eventos de negocio de baja cardinalidad para Prometheus. */
@Component
public class BusinessEventMetrics {

    private static final String METRIC_NAME = "app.business.events";
    private static final String SUCCESS = "success";

    private final MeterRegistry meterRegistry;

    public BusinessEventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void solicitudCreada() {
        registrar("solicitud_creada");
    }

    public void solicitudAprobada() {
        registrar("solicitud_aprobada");
    }

    public void solicitudRechazada() {
        registrar("solicitud_rechazada");
    }

    public void solicitudCancelada() {
        registrar("solicitud_cancelada");
    }

    public void reservaCreada() {
        registrar("reserva_creada");
    }

    public void reservaCancelada() {
        registrar("reserva_cancelada");
    }

    public void reservaFinalizada() {
        registrar("reserva_finalizada");
    }

    private void registrar(String evento) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            incrementar(evento);
                        }
                    });
            return;
        }
        incrementar(evento);
    }

    private void incrementar(String evento) {
        meterRegistry.counter(
                METRIC_NAME,
                "event", evento,
                "status", SUCCESS
        ).increment();
    }
}
