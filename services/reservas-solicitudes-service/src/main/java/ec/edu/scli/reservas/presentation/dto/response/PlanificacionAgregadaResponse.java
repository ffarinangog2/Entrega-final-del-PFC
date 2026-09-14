package ec.edu.scli.reservas.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanificacionAgregadaResponse(
        UUID id, UUID carreraId, UUID periodoId, String estado,
        UUID coordinadorPerfilId, Instant creadaEn, Instant enviadaEn,
        Instant aprobadaEn, List<PlanificacionResponse> bloques,
        List<RevisionResponse> revisiones,
        UUID pisoGestionadoId) {

    public PlanificacionAgregadaResponse(
            UUID id, UUID carreraId, UUID periodoId, String estado,
            UUID coordinadorPerfilId, Instant creadaEn, Instant enviadaEn,
            Instant aprobadaEn, List<PlanificacionResponse> bloques,
            List<RevisionResponse> revisiones) {
        this(id, carreraId, periodoId, estado, coordinadorPerfilId, creadaEn, enviadaEn, aprobadaEn, bloques, revisiones, null);
    }

    public record RevisionResponse(UUID id, UUID pisoId, String estado, String observacion,
            Integer ronda, boolean vigente, UUID revisadaPorPerfilId, Instant actualizadaEn,
            List<ObservacionResponse> observaciones) { }
    public record ObservacionResponse(UUID bloqueId, UUID laboratorioPropuestoId, String observacion) { }
}
