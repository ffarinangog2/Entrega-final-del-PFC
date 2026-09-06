package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AjustarPresenciaRequest(
        @Pattern(regexp = "PRESENTE|AUSENTE") String estado,
        @NotBlank @Size(max = 500) String observacion) { }
