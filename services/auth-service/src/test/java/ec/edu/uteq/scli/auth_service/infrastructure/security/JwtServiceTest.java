package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.infrastructure.config.JwtProperties;
import ec.edu.uteq.scli.auth_service.domain.model.Permiso;
import ec.edu.uteq.scli.auth_service.domain.model.Rol;
import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
