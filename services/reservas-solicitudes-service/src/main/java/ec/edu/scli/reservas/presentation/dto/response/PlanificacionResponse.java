package ec.edu.scli.reservas.presentation.dto.response;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record PlanificacionResponse(
        UUID id, UUID periodoId, UUID carreraId, UUID materiaId, UUID docenteId,
        UUID laboratorioId, String diaSemana, LocalTime horaInicio, LocalTime horaFin,
        String estado, String observacion, UUID creadoPorPerfilId,
        Instant creadaEn, Instant actualizadaEn, Long version) { }
