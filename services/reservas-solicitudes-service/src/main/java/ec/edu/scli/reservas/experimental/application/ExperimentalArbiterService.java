package ec.edu.scli.reservas.experimental.application;

import ec.edu.scli.reservas.experimental.domain.*;
import java.time.Instant;

public final class ExperimentalArbiterService {
    private final ArbitrajeStrategyResolver resolver;
    public ExperimentalArbiterService(ArbitrajeStrategyResolver resolver) { this.resolver = resolver; }
    public ResultadoArbitraje adjudicar(SolicitudArbitraje solicitud, String equipmentStatus, boolean equipmentActive) {
        ArbitrajeStrategy strategy = resolver.resolve();
        if (!equipmentActive || "MANTENIMIENTO".equalsIgnoreCase(equipmentStatus)
                || "FUERA_DE_SERVICIO".equalsIgnoreCase(equipmentStatus))
            return new ResultadoArbitraje(solicitud.runId(), solicitud.requestId(), strategy.nombre(), "REJECTED",
                    "EQUIPMENT_UNAVAILABLE", 0, null, null, null, Instant.now());
        return strategy.adjudicar(solicitud);
    }
    public ArbitrajeStrategy selected() { return resolver.resolve(); }
}
