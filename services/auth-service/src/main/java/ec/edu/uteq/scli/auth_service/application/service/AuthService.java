package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.infrastructure.client.UsuariosClient;
import ec.edu.uteq.scli.auth_service.presentation.dto.AuthUserResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.PerfilAuthResponse;
import ec.edu.uteq.scli.auth_service.domain.service.AccountBlockedException;
import ec.edu.uteq.scli.auth_service.domain.service.AccountDisabledException;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidCredentialsException;
import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetails;
import ec.edu.uteq.scli.auth_service.infrastructure.security.CustomUserDetailsService;
import ec.edu.uteq.scli.auth_service.infrastructure.security.JwtService;
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
        private final LoginProtectionService loginProtectionService;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        UsuariosClient usuariosClient,
                        CustomUserDetailsService customUserDetailsService,
                        LoginProtectionService loginProtectionService) {

                this.authenticationManager = authenticationManager;
                this.jwtService = jwtService;
                this.usuariosClient = usuariosClient;
                this.customUserDetailsService = customUserDetailsService;
                this.loginProtectionService = loginProtectionService;
        }

        public LoginResponse login(LoginRequest request) {
                return login(request, LoginProtectionService.LoginMetadata.unknown());
        }

        public LoginResponse login(LoginRequest request, LoginProtectionService.LoginMetadata metadata) {

                String identifier = request.username().trim();
                if (loginProtectionService.prepareLogin(identifier)) {
                        throw new AccountBlockedException();
                }

                try {

                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        identifier,
                                                        request.password()));

                        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                        loginProtectionService.recordSuccess(userDetails.getUsuarioId(), identifier, metadata);

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
                        if (loginProtectionService.recordFailure(identifier, metadata)) {
                                throw new AccountBlockedException();
                        }
                        throw new InvalidCredentialsException();
                }
        }

        public LoginResponse refrescar(String refreshToken) {

                if (!jwtService.esRefreshTokenValido(refreshToken)) {
                        throw new InvalidCredentialsException();
                }

                CustomUserDetails userDetails = customUserDetailsService.loadUserById(
                                jwtService.extraerUsuarioId(refreshToken));

                if (loginProtectionService.ensureNotLocked(userDetails.getUsuarioId())) {
                        throw new AccountBlockedException();
                }
                if (!userDetails.isEnabled()) {
                        throw new AccountDisabledException();
                }

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
