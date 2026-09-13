package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record ResponderPropuestaRequest(@Size(max = 2000) String comentario) { }
