package ec.edu.uteq.scli.auth_service.service;

import ec.edu.uteq.scli.auth_service.config.JwtProperties;
import ec.edu.uteq.scli.auth_service.entity.Permiso;
import ec.edu.uteq.scli.auth_service.entity.Rol;
import ec.edu.uteq.scli.auth_service.entity.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.security.CustomUserDetails;
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

                Permiso permiso = new Permiso();
                permiso.setId(UUID.randomUUID());
                permiso.setCodigo("USUARIO_LEER");
                permiso.setActivo(true);

                Rol rol = new Rol();
                rol.setId(UUID.randomUUID());
                rol.setCodigo("ADMINISTRADOR");
                rol.setActivo(true);
                rol.setPermisos(Set.of(permiso));

                UsuarioAuth usuario = new UsuarioAuth();
                usuario.setId(UUID.randomUUID());
                usuario.setPerfilId(UUID.randomUUID());
                usuario.setUsername("admin");
                usuario.setEmail("admin@scli.local");
                usuario.setPasswordHash("hash-prueba");
                usuario.setActivo(true);
                usuario.setCuentaBloqueada(false);
                usuario.setRoles(Set.of(rol));

                CustomUserDetails userDetails = new CustomUserDetails(usuario);

                String token = jwtService.generarAccessToken(userDetails);

                assertTrue(jwtService.esTokenValido(token));

                assertEquals(
                                usuario.getId(),
                                jwtService.extraerUsuarioId(token));

                assertEquals(
                                "admin",
                                jwtService.extraerUsername(token));

                assertFalse(token.isBlank());
        }
}