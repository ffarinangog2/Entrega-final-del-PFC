package ec.edu.uteq.scli.auth_service.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUsuarioUpdateRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank String rol,
        boolean activo) {
}
