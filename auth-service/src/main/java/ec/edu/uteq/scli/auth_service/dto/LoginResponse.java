package ec.edu.uteq.scli.auth_service.dto;

public record LoginResponse(
                String tokenType,
                String accessToken,
                String refreshToken,
                long expiresIn,
                AuthUserResponse usuario) {
}