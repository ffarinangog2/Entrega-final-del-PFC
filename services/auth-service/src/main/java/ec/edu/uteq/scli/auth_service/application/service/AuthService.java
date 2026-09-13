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
import ec.edu.uteq.scli.auth_service.infrastructure.metrics.AuthenticationMetrics;
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
        private final RefreshSessionService refreshSessionService;
        private final AuthenticationMetrics metrics;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        UsuariosClient usuariosClient,
                        CustomUserDetailsService customUserDetailsService,
                        LoginProtectionService loginProtectionService,
                        RefreshSessionService refreshSessionService,
                        AuthenticationMetrics metrics) {

                this.authenticationManager = authenticationManager;
                this.jwtService = jwtService;
                this.usuariosClient = usuariosClient;
                this.customUserDetailsService = customUserDetailsService;
                this.loginProtectionService = loginProtectionService;
                this.refreshSessionService = refreshSessionService;
                this.metrics = metrics;
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
                        JwtService.RefreshTokenInfo refreshInfo = jwtService.extraerInfoRefreshToken(refreshToken);
                        refreshSessionService.registrar(userDetails.getUsuarioId(), refreshToken,
                                        refreshInfo.emitidoEn(), refreshInfo.expiraEn());

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

                        LoginResponse response = new LoginResponse(
                                        "Bearer",
                                        accessToken,
                                        refreshToken,
                                        jwtService.obtenerExpiracionAccessTokenSegundos(),
                                        usuario);
                        metrics.authenticationSuccess();
                        return response;

                } catch (LockedException exception) {
                        metrics.authenticationFailure();
                        throw new AccountBlockedException();

                } catch (DisabledException exception) {
                        metrics.authenticationFailure();
                        throw new AccountDisabledException();

                } catch (BadCredentialsException exception) {
                        metrics.authenticationFailure();
                        if (loginProtectionService.recordFailure(identifier, metadata)) {
                                throw new AccountBlockedException();
                        }
                        throw new InvalidCredentialsException();
                }
        }

        public LoginResponse refrescar(String refreshToken) {
                JwtService.RefreshTokenInfo refreshInfo = validarRefreshToken(refreshToken);
                var sessionAnterior = refreshSessionService.validarActiva(refreshInfo.usuarioId(), refreshToken);

                CustomUserDetails userDetails = customUserDetailsService.loadUserById(
                                refreshInfo.usuarioId());

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
                JwtService.RefreshTokenInfo nuevaInfo = jwtService.extraerInfoRefreshToken(nuevoRefreshToken);
                refreshSessionService.rotar(sessionAnterior, refreshToken, nuevoRefreshToken,
                                nuevaInfo.emitidoEn(), nuevaInfo.expiraEn());

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

                LoginResponse response = new LoginResponse(
                                "Bearer",
                                nuevoAccessToken,
                                nuevoRefreshToken,
                                jwtService.obtenerExpiracionAccessTokenSegundos(),
                                usuario);
                metrics.tokenRefresh();
                return response;
        }

        public void logout(String refreshToken) {
                validarRefreshToken(refreshToken);
                refreshSessionService.revocarIdempotente(refreshToken);
                metrics.logout();
        }

        private JwtService.RefreshTokenInfo validarRefreshToken(String token) {
                try {
                        return jwtService.extraerInfoRefreshToken(token);
                } catch (RuntimeException exception) {
                        throw new InvalidCredentialsException();
                }
        }
}
