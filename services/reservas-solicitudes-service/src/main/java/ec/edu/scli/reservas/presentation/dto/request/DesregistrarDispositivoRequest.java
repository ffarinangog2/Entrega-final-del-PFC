package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DesregistrarDispositivoRequest(
        @NotBlank @Size(max = 4096) String token) {
}
