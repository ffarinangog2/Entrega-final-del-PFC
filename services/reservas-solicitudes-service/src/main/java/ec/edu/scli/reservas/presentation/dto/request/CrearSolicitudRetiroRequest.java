package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CrearSolicitudRetiroRequest(@NotBlank String motivo) { }
