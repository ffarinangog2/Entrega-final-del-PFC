package ec.edu.uteq.scli.auth_service.service;

import ec.edu.uteq.scli.auth_service.config.JwtProperties;
import ec.edu.uteq.scli.auth_service.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

        private final JwtProperties jwtProperties;
        private final SecretKey signingKey;

        public JwtService(JwtProperties jwtProperties) {
                this.jwtProperties = jwtProperties;

                byte[] keyBytes = Decoders.BASE64.decode(
                                jwtProperties.secret());

                if (keyBytes.length < 32) {
                        throw new IllegalArgumentException(
                                        "JWT_SECRET debe representar al menos 32 bytes");
                }

                this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }

        public String generarAccessToken(
                        CustomUserDetails userDetails) {
                Instant ahora = Instant.now();

                Instant expiracion = ahora.plusMillis(
                                jwtProperties.accessTokenExpirationMs());

                List<String> roles = userDetails
                                .getAuthorities()
                                .stream()
                                .map(authority -> authority.getAuthority())
                                .filter(authority -> authority.startsWith("ROLE_"))
                                .map(authority -> authority.substring("ROLE_".length()))
                                .sorted()
                                .toList();

                List<String> permisos = userDetails
                                .getAuthorities()
                                .stream()
                                .map(authority -> authority.getAuthority())
                                .filter(authority -> !authority.startsWith("ROLE_"))
                                .sorted()
                                .toList();

                return Jwts.builder()
                                .issuer(jwtProperties.issuer())
                                .subject(userDetails.getUsuarioId().toString())
                                .issuedAt(Date.from(ahora))
                                .expiration(Date.from(expiracion))
                                .id(UUID.randomUUID().toString())
                                .claim(
                                                "username",
                                                userDetails.getUsername())
                                .claim(
                                                "perfilId",
                                                userDetails.getPerfilId().toString())
                                .claim("type", "access")
                                .claim("roles", roles)
                                .claim("permissions", permisos)
                                .signWith(signingKey)
                                .compact();
        }

        public String generarRefreshToken(
                        CustomUserDetails userDetails) {

                Instant ahora = Instant.now();

                Instant expiracion = ahora.plusMillis(
                                jwtProperties.refreshTokenExpirationMs());

                return Jwts.builder()
                                .issuer(jwtProperties.issuer())
                                .subject(userDetails.getUsuarioId().toString())
                                .issuedAt(Date.from(ahora))
                                .expiration(Date.from(expiracion))
                                .id(UUID.randomUUID().toString())
                                .claim(
                                                "username",
                                                userDetails.getUsername())
                                .claim(
                                                "perfilId",
                                                userDetails.getPerfilId().toString())
                                .claim("type", "refresh")
                                .signWith(signingKey)
                                .compact();
        }

        public Claims extraerClaims(String token) {
                return Jwts.parser()
                                .verifyWith(signingKey)
                                .requireIssuer(jwtProperties.issuer())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
        }

        public UUID extraerUsuarioId(String token) {
                String subject = extraerClaims(token).getSubject();

                return UUID.fromString(subject);
        }

        public String extraerUsername(String token) {
                return extraerClaims(token)
                                .get("username", String.class);
        }

        public boolean esTokenValido(String token) {

                try {

                        Claims claims = extraerClaims(token);

                        return "access".equals(
                                        claims.get("type", String.class))
                                        && claims.getExpiration().after(new Date());

                } catch (
                                JwtException
                                | IllegalArgumentException exception) {

                        return false;
                }
        }

        public boolean esRefreshTokenValido(String token) {

                try {

                        Claims claims = extraerClaims(token);

                        return "refresh".equals(
                                        claims.get("type", String.class))
                                        && claims.getExpiration().after(new Date());

                } catch (JwtException | IllegalArgumentException ex) {

                        return false;
                }
        }

        public long obtenerExpiracionAccessTokenSegundos() {
                return jwtProperties.accessTokenExpirationMs() / 1000;
        }

        public long obtenerExpiracionRefreshTokenSegundos() {
                return jwtProperties.refreshTokenExpirationMs() / 1000;
        }
}