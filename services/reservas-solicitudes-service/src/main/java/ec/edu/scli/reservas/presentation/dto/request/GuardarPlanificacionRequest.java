package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record GuardarPlanificacionRequest(
        UUID planificacionId,
        @NotNull Integer nivel,
        @NotNull UUID periodoId,
        @NotNull UUID carreraId,
        @NotNull UUID materiaId,
        UUID docenteId,
        @NotNull UUID laboratorioId,
        @NotBlank String diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String observacion) {
    public GuardarPlanificacionRequest(UUID periodoId, UUID carreraId, UUID materiaId,
            UUID docenteId, UUID laboratorioId, String diaSemana, LocalTime horaInicio,
            LocalTime horaFin, String observacion) {
        this(null, 1, periodoId, carreraId, materiaId, docenteId, laboratorioId,
                diaSemana, horaInicio, horaFin, observacion);
    }
}
