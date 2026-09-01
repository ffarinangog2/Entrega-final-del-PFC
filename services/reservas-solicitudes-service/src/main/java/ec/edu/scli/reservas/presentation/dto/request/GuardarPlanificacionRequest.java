package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record GuardarPlanificacionRequest(
        @NotNull UUID periodoId,
        @NotNull UUID carreraId,
        @NotNull UUID materiaId,
        UUID docenteId,
        @NotNull UUID laboratorioId,
        @NotBlank String diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String observacion) { }
