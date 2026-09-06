package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletarUsoLaboratorioRequest(
        @NotBlank @Size(max = 500) String temaActividad,
        @Size(max = 2000) String observacionUso) { }
