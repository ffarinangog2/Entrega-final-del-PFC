package ec.edu.scli.reservas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Valida y traduce los access tokens generados por auth-service. */
@Component
public class JwtTokenProvider {

    private final String issuer;
    private final SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.secret}") String secret) {
        this.issuer = issuer;

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET debe representar al menos 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtAuthenticationData parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID usuarioAuthId = requiredUuid(claims.getSubject(), "sub");
        UUID perfilId = requiredUuid(
                claims.get("perfilId", String.class), "perfilId");
        String username = claims.get("username", String.class);

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        claimValues(claims, "roles").stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        claimValues(claims, "permissions").stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        JwtPrincipal principal = new JwtPrincipal(
                usuarioAuthId, perfilId, username);
        return new JwtAuthenticationData(principal, List.copyOf(authorities));
    }

    private UUID requiredUuid(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "El token no contiene el claim obligatorio " + claimName);
        }
        return UUID.fromString(value);
    }

    private List<String> claimValues(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (!(value instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(item -> !item.isBlank())
                .toList();
    }

    public record JwtAuthenticationData(
            JwtPrincipal principal,
            List<GrantedAuthority> authorities
    ) {
    }
}
