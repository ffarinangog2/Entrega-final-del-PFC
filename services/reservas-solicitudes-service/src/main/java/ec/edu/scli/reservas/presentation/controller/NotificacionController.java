package ec.edu.scli.reservas.presentation.controller;
import ec.edu.scli.reservas.application.service.NotificacionService;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.request.DesregistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import ec.edu.scli.reservas.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notificaciones/dispositivos")
public class NotificacionController {
    private final NotificacionService service; public NotificacionController(NotificacionService s){service=s;}
    @PostMapping public ResponseEntity<DispositivoNotificacionResponse> registrar(@Valid @RequestBody RegistrarDispositivoRequest request,Principal principal){
        JwtPrincipal jwt=(JwtPrincipal)principal;
        return ResponseEntity.ok(service.registrar(request,jwt.usuarioAuthId(),jwt.perfilId()));
    }
    @DeleteMapping public ResponseEntity<Void> desregistrar(@Valid @RequestBody DesregistrarDispositivoRequest request, Principal principal) {
        JwtPrincipal jwt = (JwtPrincipal) principal;
        service.desregistrar(request.token(), jwt.perfilId());
        return ResponseEntity.noContent().build();
    }
}
