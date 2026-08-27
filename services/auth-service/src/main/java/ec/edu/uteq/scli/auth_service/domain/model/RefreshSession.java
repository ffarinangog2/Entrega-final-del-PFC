package ec.edu.uteq.scli.auth_service.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        UUID usuarioId,
        String tokenHash,
        UUID familiaToken,
        OffsetDateTime emitidoEn,
        OffsetDateTime expiraEn,
        boolean revocado,
        OffsetDateTime revocadoEn,
        UUID reemplazadoPor) {
}
