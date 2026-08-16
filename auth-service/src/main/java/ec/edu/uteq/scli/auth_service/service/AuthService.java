package ec.edu.uteq.scli.auth_service.service;

import ec.edu.uteq.scli.auth_service.client.UsuariosClient;
import ec.edu.uteq.scli.auth_service.dto.AuthUserResponse;
import ec.edu.uteq.scli.auth_service.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.dto.PerfilAuthResponse;
import ec.edu.uteq.scli.auth_service.exception.AccountBlockedException;
import ec.edu.uteq.scli.auth_service.exception.AccountDisabledException;
import ec.edu.uteq.scli.auth_service.exception.InvalidCredentialsException;
import ec.edu.uteq.scli.auth_service.security.CustomUserDetails;
import ec.edu.uteq.scli.auth_service.security.CustomUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;
        private final UsuariosClient usuariosClient;
        private final CustomUserDetailsService customUserDetailsService;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        UsuariosClient usuariosClient,
                        CustomUserDetailsService customUserDetailsService) {

                this.authenticationManager = authenticationManager;
                this.jwtService = jwtService;
                this.usuariosClient = usuariosClient;
                this.customUserDetailsService = customUserDetailsService;
        }

        public LoginResponse login(LoginRequest request) {

                try {

                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.username().trim(),
                                                        request.password()));

                        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                        PerfilAuthResponse perfil = usuariosClient.obtenerPerfil(
                                        userDetails.getPerfilId());

                        if (Boolean.FALSE.equals(perfil.activo())) {
                                throw new AccountDisabledException();
                        }

                        String accessToken = jwtService.generarAccessToken(userDetails);

                        String refreshToken = jwtService.generarRefreshToken(userDetails);

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

                        AuthUserResponse usuario = new AuthUserResponse(
                                        userDetails.getUsuarioId(),
                                        userDetails.getPerfilId(),
                                        userDetails.getUsername(),
                                        perfil.nombres(),
                                        perfil.apellidos(),
                                        perfil.emailInstitucional(),
                                        roles,
                                        permisos,
                                        perfil.tiposPerfil());

                        return new LoginResponse(
                                        "Bearer",
                                        accessToken,
                                        refreshToken,
                                        jwtService.obtenerExpiracionAccessTokenSegundos(),
                                        usuario);

                } catch (LockedException exception) {
                        throw new AccountBlockedException();

                } catch (DisabledException exception) {
                        throw new AccountDisabledException();

                } catch (BadCredentialsException exception) {
                        throw new InvalidCredentialsException();
                }
        }

        public LoginResponse refrescar(String refreshToken) {

                if (!jwtService.esRefreshTokenValido(refreshToken)) {
                        throw new InvalidCredentialsException();
                }

                CustomUserDetails userDetails = customUserDetailsService.loadUserById(
                                jwtService.extraerUsuarioId(refreshToken));

                PerfilAuthResponse perfil = usuariosClient.obtenerPerfil(
                                userDetails.getPerfilId());

                if (Boolean.FALSE.equals(perfil.activo())) {
                        throw new AccountDisabledException();
                }

                String nuevoAccessToken = jwtService.generarAccessToken(userDetails);

                String nuevoRefreshToken = jwtService.generarRefreshToken(userDetails);

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

                AuthUserResponse usuario = new AuthUserResponse(
                                userDetails.getUsuarioId(),
                                userDetails.getPerfilId(),
                                userDetails.getUsername(),
                                perfil.nombres(),
                                perfil.apellidos(),
                                perfil.emailInstitucional(),
                                roles,
                                permisos,
                                perfil.tiposPerfil());

                return new LoginResponse(
                                "Bearer",
                                nuevoAccessToken,
                                nuevoRefreshToken,
                                jwtService.obtenerExpiracionAccessTokenSegundos(),
                                usuario);
        }
}