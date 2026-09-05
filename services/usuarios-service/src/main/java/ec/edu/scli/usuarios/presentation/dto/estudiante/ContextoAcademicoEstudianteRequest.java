package ec.edu.scli.usuarios.presentation.dto.estudiante;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ContextoAcademicoEstudianteRequest(@NotNull UUID carreraId, @NotNull UUID periodoId,
        @NotNull @Min(1) @Max(10) Integer nivel) { }
