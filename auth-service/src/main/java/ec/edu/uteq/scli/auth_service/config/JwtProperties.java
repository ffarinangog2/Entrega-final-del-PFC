package ec.edu.uteq.scli.auth_service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(

        @NotBlank String issuer,

        @NotBlank String secret,

        @Min(60000) long accessTokenExpirationMs,

        @Min(60000) long refreshTokenExpirationMs) {
}