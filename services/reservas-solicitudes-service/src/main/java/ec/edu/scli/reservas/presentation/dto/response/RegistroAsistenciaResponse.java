package ec.edu.scli.reservas.presentation.dto.response;
import java.time.Instant; import java.util.UUID;
public record RegistroAsistenciaResponse(UUID id, UUID sesionId, UUID estudianteId, UUID bloqueId,
        Instant registradaEn, String estado) {
    public RegistroAsistenciaResponse(UUID id, UUID sesionId, UUID estudianteId, Instant registradaEn, String estado) {
        this(id, sesionId, estudianteId, null, registradaEn, estado);
    }
}
