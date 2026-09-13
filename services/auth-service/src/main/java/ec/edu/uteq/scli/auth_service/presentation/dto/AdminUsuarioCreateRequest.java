package ec.edu.uteq.scli.auth_service.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdminUsuarioCreateRequest(
        @NotNull UUID perfilId,
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank String passwordInicial,
        @NotBlank String rol) {
}
