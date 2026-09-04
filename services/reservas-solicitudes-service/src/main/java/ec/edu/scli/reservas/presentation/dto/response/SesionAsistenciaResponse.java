package ec.edu.scli.reservas.presentation.dto.response;
import java.time.*; import java.util.UUID;
public record SesionAsistenciaResponse(UUID id, UUID reservaId, UUID bloqueId, LocalDate fechaClase,
        Instant abiertaEn, Instant expiraEn, String estado, String token) {}
