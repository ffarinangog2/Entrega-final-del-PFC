package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.presentation.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.application.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ec.edu.uteq.scli.auth_service.presentation.dto.RefreshTokenRequest;
import ec.edu.uteq.scli.auth_service.application.service.LoginProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import ec.edu.uteq.scli.auth_service.application.service.PasswordRecoveryService;
import ec.edu.uteq.scli.auth_service.presentation.dto.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                authService.login(request, new LoginProtectionService.LoginMetadata(
                        clientIp(httpRequest), httpRequest.getHeader("User-Agent"))));
    }

    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        boolean trustedProxy = remote != null && (remote.equals("127.0.0.1") || remote.equals("::1")
                || remote.startsWith("10.") || remote.startsWith("172.") || remote.startsWith("192.168."));
        if (trustedProxy && forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim();
        return remote;
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(

            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                authService.refrescar(request.refreshToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        passwordRecoveryService.request(request.identifier(), clientIp(httpRequest));
        return ResponseEntity.ok(new MessageResponse(PasswordRecoveryService.NEUTRAL_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.reset(request.token(), request.newPassword(), request.confirmPassword());
        return ResponseEntity.ok(new MessageResponse("La contraseña se actualizó correctamente."));
    }

}
