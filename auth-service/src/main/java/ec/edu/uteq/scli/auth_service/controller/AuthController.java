package ec.edu.uteq.scli.auth_service.controller;

import ec.edu.uteq.scli.auth_service.dto.LoginRequest;
import ec.edu.uteq.scli.auth_service.dto.LoginResponse;
import ec.edu.uteq.scli.auth_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ec.edu.uteq.scli.auth_service.dto.RefreshTokenRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(

            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(
                authService.refrescar(request.refreshToken()));
    }

}