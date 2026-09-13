package ec.edu.scli.reservas.presentation.dto.request;

import ec.edu.scli.reservas.domain.model.PrioridadIncidente;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CrearIncidenteRequest(
        @NotBlank @Size(max=200) String laboratorioEquipo,
        @NotBlank @Size(max=2000) String descripcion,
        @NotNull PrioridadIncidente prioridad,
        @NotNull @PastOrPresent LocalDate fecha) { }
