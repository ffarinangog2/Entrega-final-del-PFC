package ec.edu.scli.reservas.presentation.dto.request;
import jakarta.validation.constraints.*;
public record RegistrarDispositivoRequest(@NotBlank @Size(max=4096) String token,
        @NotBlank @Pattern(regexp="ANDROID") String plataforma) { }
