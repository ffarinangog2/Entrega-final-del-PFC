package ec.edu.uteq.scli.auth_service.presentation.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(max=64) String newPassword,
                                   @NotBlank @Size(max=64) String confirmPassword) { }
