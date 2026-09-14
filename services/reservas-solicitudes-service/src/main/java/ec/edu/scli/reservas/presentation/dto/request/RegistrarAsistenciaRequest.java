package ec.edu.scli.reservas.presentation.dto.request;
import jakarta.validation.constraints.NotBlank;
public record RegistrarAsistenciaRequest(@NotBlank String token) {}
