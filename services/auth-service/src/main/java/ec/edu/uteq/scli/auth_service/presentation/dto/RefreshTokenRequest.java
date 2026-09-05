package ec.edu.uteq.scli.auth_service.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank String refreshToken) {
}
