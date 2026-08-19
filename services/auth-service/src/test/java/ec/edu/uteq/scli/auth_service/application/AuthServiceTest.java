package ec.edu.uteq.scli.auth_service.application;

import ec.edu.uteq.scli.auth_service.domain.service.AccountBlockedException;
import ec.edu.uteq.scli.auth_service.domain.service.AccountDisabledException;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidCredentialsException;
import ec.edu.uteq.scli.auth_service.infrastructure.client.UsuariosClient;
import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetails;
import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetailsService;
// AGREGAR
import ec.edu.uteq.scli.auth_service.application.service.AuthService;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtService;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.PerfilAuthResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private JwtService jwtService;

        @Mock
        private UsuariosClient usuariosClient;

        @Mock
        private CustomUserDetailsService customUserDetailsService;

        @Mock
        private Authentication authentication;

        @InjectMocks
        private AuthService authService;

        private CustomUserDetails userDetails;
        private UUID usuarioId;
        private UUID perfilId;

        @BeforeEach
        void setUp() {

                usuarioId = UUID.randomUUID();
                perfilId = UUID.randomUUID();

                var usuario = new ec.edu.uteq.scli.auth_service.domain.model.Usuario(
                                usuarioId,
                                perfilId,
                                "ivan",
                                "ivan@uteq.edu.ec",
                                "password-hash",
                                true,
                                false,
                                Set.of());

                userDetails = new CustomUserDetails(usuario);
        }

        @Test
        void loginExitosoDebeGenerarTokens() {

                LoginRequest request = new LoginRequest(
                                "ivan",
                                "password");

                PerfilAuthResponse perfil = new PerfilAuthResponse(
                                perfilId,
                                "Ivan",
                                "Villamarin",
                                "ivan@uteq.edu.ec",
                                true,
                                List.of());

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);

                when(authentication.getPrincipal())
                                .thenReturn(userDetails);

                when(usuariosClient.obtenerPerfil(perfilId))
                                .thenReturn(perfil);

                when(jwtService.generarAccessToken(userDetails))
                                .thenReturn("access-token");

                when(jwtService.generarRefreshToken(userDetails))
                                .thenReturn("refresh-token");

                when(jwtService.obtenerExpiracionAccessTokenSegundos())
                                .thenReturn(900L);

                LoginResponse response = authService.login(request);

                assertNotNull(response);
                assertEquals("Bearer", response.tokenType());
                assertEquals("access-token", response.accessToken());
                assertEquals("refresh-token", response.refreshToken());
                assertEquals(900L, response.expiresIn());

                verify(authenticationManager)
                                .authenticate(any(UsernamePasswordAuthenticationToken.class));

                verify(jwtService).generarAccessToken(userDetails);
                verify(jwtService).generarRefreshToken(userDetails);
        }

        @Test
        void loginConCredencialesInvalidasDebeLanzarExcepcion() {

                LoginRequest request = new LoginRequest(
                                "ivan",
                                "incorrecta");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));

                verify(jwtService, never()).generarAccessToken(any());
        }

        @Test
        void loginConCuentaBloqueadaDebeLanzarExcepcion() {

                LoginRequest request = new LoginRequest(
                                "ivan",
                                "password");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new LockedException("Cuenta bloqueada"));

                assertThrows(
                                AccountBlockedException.class,
                                () -> authService.login(request));
        }

        @Test
        void loginConCuentaDeshabilitadaDebeLanzarExcepcion() {

                LoginRequest request = new LoginRequest(
                                "ivan",
                                "password");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new DisabledException("Cuenta deshabilitada"));

                assertThrows(
                                AccountDisabledException.class,
                                () -> authService.login(request));
        }

        @Test
        void loginConPerfilDeshabilitadoDebeLanzarExcepcion() {

                LoginRequest request = new LoginRequest(
                                "ivan",
                                "password");

                PerfilAuthResponse perfil = new PerfilAuthResponse(
                                perfilId,
                                "Ivan",
                                "Villamarin",
                                "ivan@uteq.edu.ec",
                                false,
                                List.of());

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);

                when(authentication.getPrincipal())
                                .thenReturn(userDetails);

                when(usuariosClient.obtenerPerfil(perfilId))
                                .thenReturn(perfil);

                assertThrows(
                                AccountDisabledException.class,
                                () -> authService.login(request));

                verify(jwtService, never()).generarAccessToken(any());
        }

        @Test
        void refreshTokenInvalidoDebeLanzarExcepcion() {

                when(jwtService.esRefreshTokenValido("refresh-invalido"))
                                .thenReturn(false);

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.refrescar("refresh-invalido"));

                verify(customUserDetailsService, never()).loadUserById(any());
        }
}