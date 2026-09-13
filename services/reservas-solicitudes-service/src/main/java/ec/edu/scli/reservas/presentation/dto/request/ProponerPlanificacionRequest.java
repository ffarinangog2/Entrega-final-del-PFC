package ec.edu.scli.reservas.presentation.dto.request;

import java.time.LocalTime;
import java.util.UUID;

public record ProponerPlanificacionRequest(
        UUID laboratorioId, LocalTime horaInicio, LocalTime horaFin, String observacion) { }
