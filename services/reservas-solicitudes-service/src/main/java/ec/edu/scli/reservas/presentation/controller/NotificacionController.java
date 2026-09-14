package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.NotificacionService;
import ec.edu.scli.reservas.presentation.dto.request.DesregistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import ec.edu.scli.reservas.presentation.dto.response.NotificacionInternaResponse;
import ec.edu.scli.reservas.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService s) {
        service = s;
    }

    @PostMapping("/dispositivos")
    public ResponseEntity<DispositivoNotificacionResponse> registrar(
            @Valid @RequestBody RegistrarDispositivoRequest request, Principal principal) {
        JwtPrincipal jwt = jwt(principal);
        return ResponseEntity.ok(service.registrar(request, jwt.usuarioAuthId(), jwt.perfilId()));
    }

    @DeleteMapping("/dispositivos")
    public ResponseEntity<Void> desregistrar(
            @Valid @RequestBody DesregistrarDispositivoRequest request, Principal principal) {
        JwtPrincipal jwt = jwt(principal);
        service.desregistrar(request.token(), jwt.perfilId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<NotificacionInternaResponse> listar(Principal principal) {
        return service.listar(perfil(principal));
    }

    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas(Principal principal) {
        return Map.of("cantidad", service.noLeidas(perfil(principal)));
    }

    @PostMapping("/{id}/leer")
    public NotificacionInternaResponse leer(@PathVariable UUID id, Principal principal) {
        return service.leer(id, perfil(principal));
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<Void> leerTodas(Principal principal) {
        service.leerTodas(perfil(principal));
        return ResponseEntity.noContent().build();
    }

    private JwtPrincipal jwt(Principal principal) {
        if (principal instanceof JwtPrincipal jp) {
            return jp;
        }
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof JwtPrincipal jp) {
            return jp;
        }
        throw new AccessDeniedException("No existe un actor JWT autenticado");
    }

    private UUID perfil(Principal principal) {
        return jwt(principal).perfilId();
    }
}
