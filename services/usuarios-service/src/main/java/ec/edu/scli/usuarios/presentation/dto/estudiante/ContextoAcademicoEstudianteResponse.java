package ec.edu.scli.usuarios.presentation.dto.estudiante;
import java.time.OffsetDateTime;
import java.util.UUID;
public record ContextoAcademicoEstudianteResponse(UUID id, UUID estudianteId, UUID carreraId,
        UUID periodoId, Integer nivel, boolean activo, OffsetDateTime creadoEn) { }
