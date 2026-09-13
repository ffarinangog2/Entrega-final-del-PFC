package ec.edu.scli.reservas.experimental.presentation;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record AdjudicacionExperimentalRequest(@NotBlank String runId, @NotBlank String requestId,
        @NotNull UUID equipmentId, @NotNull UUID laboratorioId, @NotNull UUID agenteId,
        @NotNull Instant inicio, @NotNull Instant fin, @NotBlank String equipmentStatus,
        boolean equipmentActive, @NotBlank String equipmentSource) { }
