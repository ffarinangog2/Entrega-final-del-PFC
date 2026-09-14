package ec.edu.scli.reservas.experimental.domain;

import java.time.Instant;
import java.util.UUID;

public record SolicitudArbitraje(String runId, String requestId, UUID equipmentId,
        UUID laboratorioId, UUID agenteId, Instant inicio, Instant fin) {
    public SolicitudArbitraje {
        if (runId == null || runId.isBlank() || requestId == null || requestId.isBlank())
            throw new IllegalArgumentException("runId y requestId son obligatorios");
        if (equipmentId == null || laboratorioId == null || agenteId == null || inicio == null || fin == null)
            throw new IllegalArgumentException("La adjudicación experimental está incompleta");
        if (!inicio.isBefore(fin)) throw new IllegalArgumentException("La franja experimental es inválida");
    }
}
