package ec.edu.uteq.scli.auth_service.presentation.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ForgotPasswordRequest(@NotBlank @Size(max=160) String identifier) { }
