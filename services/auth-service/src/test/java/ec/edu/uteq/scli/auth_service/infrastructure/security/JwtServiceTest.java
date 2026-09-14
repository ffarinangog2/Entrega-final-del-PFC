package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.infrastructure.config.JwtProperties;
import ec.edu.uteq.scli.auth_service.domain.model.Permiso;
import ec.edu.uteq.scli.auth_service.domain.model.Rol;
import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

        private static final String SECRET_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

        @Test
        void debeGenerarYValidarAccessToken() {

                JwtProperties properties = new JwtProperties(
                                "scli-auth-service",
                                SECRET_BASE64,
                                900000,
                                604800000);

                JwtService jwtService = new JwtService(properties);

                Permiso permiso = new Permiso(UUID.randomUUID(), "USUARIO_LEER", "Leer usuario", null, true);
                Rol rol = new Rol(UUID.randomUUID(), "ADMINISTRADOR", "Administrador", null, true, Set.of(permiso));
                Usuario usuario = new Usuario(UUID.randomUUID(), UUID.randomUUID(), "admin", "admin@scli.local",
                                "hash-prueba", true, false, Set.of(rol));

                CustomUserDetails userDetails = new CustomUserDetails(usuario);

                String token = jwtService.generarAccessToken(userDetails);

                assertTrue(jwtService.esTokenValido(token));

                assertEquals(
                                usuario.id(),
                                jwtService.extraerUsuarioId(token));

                assertEquals(
                                "admin",
                                jwtService.extraerUsername(token));

                assertFalse(token.isBlank());
        }

        @Test
        void debeGenerarYValidarRefreshToken() {
                JwtService jwtService = new JwtService(properties());
                CustomUserDetails userDetails = userDetails();

                String token = jwtService.generarRefreshToken(userDetails);

                assertTrue(jwtService.esRefreshTokenValido(token));
                assertFalse(jwtService.esTokenValido(token));
                assertEquals(604800L, jwtService.obtenerExpiracionRefreshTokenSegundos());
                assertEquals(userDetails.getUsuarioId(), jwtService.extraerInfoRefreshToken(token).usuarioId());
        }

        @Test
        void rechazaTokensInvalidosYSecretosInseguros() {
                JwtService jwtService = new JwtService(properties());

                assertFalse(jwtService.esTokenValido("token-invalido"));
                assertFalse(jwtService.esRefreshTokenValido("token-invalido"));
                assertThrows(IllegalArgumentException.class, () -> new JwtService(
                                new JwtProperties("issuer", "MDEyMzQ1Njc4OWFiY2RlZg==", 1000, 1000)));
        }

        private static JwtProperties properties() {
                return new JwtProperties("scli-auth-service", SECRET_BASE64, 900000, 604800000);
        }

        private static CustomUserDetails userDetails() {
                Usuario usuario = new Usuario(UUID.randomUUID(), UUID.randomUUID(), "admin", "admin@scli.local",
                                "hash-prueba", true, false, Set.of());
                return new CustomUserDetails(usuario);
        }

        @Test
        void accessTokenDebeExponerNuevosRolesYPermisosSinCambiarFormato() {
                JwtProperties properties = new JwtProperties(
                                "scli-auth-service",
                                SECRET_BASE64,
                                900000,
                                604800000);
                JwtService jwtService = new JwtService(properties);

                Permiso lecturaAcademica = new Permiso(
                                UUID.randomUUID(), "ACADEMICO_LEER",
                                "Consultar informacion academica", null, true);
                Permiso planificacion = new Permiso(
                                UUID.randomUUID(), "PLANIFICACION_GESTIONAR",
                                "Gestionar planificacion", null, true);
                Rol coordinador = new Rol(
                                UUID.randomUUID(), "COORDINADOR", "Coordinador",
                                null, true, Set.of(lecturaAcademica, planificacion));
                Usuario usuario = new Usuario(
                                UUID.randomUUID(), UUID.randomUUID(), "coordinacion",
                                "coordinacion@scli.local", "hash-prueba", true, false,
                                Set.of(coordinador));

                String token = jwtService.generarAccessToken(new CustomUserDetails(usuario));
                var claims = jwtService.extraerClaims(token);

                assertEquals("access", claims.get("type", String.class));
                assertEquals(List.of("COORDINADOR"), claims.get("roles", List.class));
                assertEquals(
                                List.of("ACADEMICO_LEER", "PLANIFICACION_GESTIONAR"),
                                claims.get("permissions", List.class));
        }
}
