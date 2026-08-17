package ec.edu.scli.reservas;

import ec.edu.scli.reservas.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservasSolicitudesServiceApplicationTests {

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String ISSUER = "scli-auth-service";

    @Test
    void jwtValidoConstruyePrincipalYAutoridades() {
        UUID usuarioAuthId = UUID.randomUUID();
        UUID perfilId = UUID.randomUUID();
        String token = crearToken(usuarioAuthId, perfilId, ISSUER);

        JwtTokenProvider.JwtAuthenticationData resultado =
                new JwtTokenProvider(ISSUER, SECRET).parse(token);

        assertThat(resultado.principal().usuarioAuthId()).isEqualTo(usuarioAuthId);
        assertThat(resultado.principal().perfilId()).isEqualTo(perfilId);
        assertThat(resultado.authorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_DOCENTE", "RESERVAS_CREAR");
    }

    @Test
    void jwtConIssuerIncorrectoEsRechazado() {
        String token = crearToken(
                UUID.randomUUID(), UUID.randomUUID(), "otro-emisor");
        JwtTokenProvider provider = new JwtTokenProvider(ISSUER, SECRET);

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    private String crearToken(UUID usuarioAuthId, UUID perfilId, String issuer) {
        return Jwts.builder()
                .subject(usuarioAuthId.toString())
                .issuer(issuer)
                .claim("perfilId", perfilId.toString())
                .claim("username", "docente@scli.edu.ec")
                .claim("roles", List.of("DOCENTE"))
                .claim("permissions", List.of("RESERVAS_CREAR"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(
                        "0123456789abcdef0123456789abcdef"
                                .getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
