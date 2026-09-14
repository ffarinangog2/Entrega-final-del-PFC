package ec.edu.scli.reservas.presentation.dto.request;

import ec.edu.scli.reservas.domain.model.EstadoIncidente;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoIncidenteRequest(@NotNull EstadoIncidente estado) { }
