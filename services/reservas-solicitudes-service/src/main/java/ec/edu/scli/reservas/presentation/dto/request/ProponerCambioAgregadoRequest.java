package ec.edu.scli.reservas.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProponerCambioAgregadoRequest(
        @NotNull UUID bloqueId, UUID laboratorioPropuestoId, @NotBlank String observacion) { }
