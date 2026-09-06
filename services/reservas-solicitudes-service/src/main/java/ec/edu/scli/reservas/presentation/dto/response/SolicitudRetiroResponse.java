package ec.edu.scli.reservas.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SolicitudRetiroResponse(UUID id, UUID planificacionId, UUID solicitantePerfilId,
        String motivo, String estado, Instant creadaEn, Instant resueltaEn,
        long pisosAprobados, int totalPisos, List<Decision> decisiones) {
    public record Decision(UUID pisoId, String estado, UUID revisadaPorPerfilId,
            String observacion, Instant creadaEn, Instant resueltaEn) { }
}
