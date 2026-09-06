package ec.edu.scli.reservas.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ParticipanteUsoResponse(UUID id, UUID sesionId, UUID estudiantePerfilId,
        String estado, Instant registradoEn, UUID registradoPorPerfilId, String observacion) { }
