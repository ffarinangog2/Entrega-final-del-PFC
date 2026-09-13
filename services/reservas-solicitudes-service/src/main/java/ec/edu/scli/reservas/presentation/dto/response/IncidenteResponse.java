package ec.edu.scli.reservas.presentation.dto.response;

import ec.edu.scli.reservas.domain.model.*;
import java.time.*;
import java.util.UUID;

public record IncidenteResponse(UUID id, UUID reportanteId, String laboratorioEquipo,
        String descripcion, PrioridadIncidente prioridad, LocalDate fecha,
        EstadoIncidente estado, Instant creadoEn, Instant actualizadoEn, Long version) { }
