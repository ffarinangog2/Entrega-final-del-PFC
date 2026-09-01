package ec.edu.scli.reservas.presentation.dto.response;
import java.time.Instant; import java.util.UUID;
public record SesionAsistenciaResponse(UUID id, UUID reservaId, Instant abiertaEn, Instant expiraEn, String estado, String token) {}
