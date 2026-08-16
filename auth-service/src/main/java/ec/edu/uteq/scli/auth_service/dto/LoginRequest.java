package ec.edu.uteq.scli.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El usuario o correo es obligatorio") @Size(max = 160, message = "El usuario o correo no puede superar 160 caracteres") String username,

        @NotBlank(message = "La contraseña es obligatoria") @Size(max = 100, message = "La contraseña no puede superar 100 caracteres") String password) {
}