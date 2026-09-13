package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.IncidenteService;
import ec.edu.scli.reservas.presentation.dto.request.*;
import ec.edu.scli.reservas.presentation.dto.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
import java.security.Principal;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController @RequestMapping("/api/v1/incidentes")
public class IncidenteController {
    private final IncidenteService service;
    public IncidenteController(IncidenteService service) { this.service=service; }

    @PostMapping
    public ResponseEntity<IncidenteResponse> crear(@Valid @RequestBody CrearIncidenteRequest request, Principal principal) {
        var response=service.crear(request,actor(principal));
        return ResponseEntity.created(URI.create("/api/v1/incidentes/"+response.id())).body(response);
    }
    @GetMapping
    public ResponseEntity<PaginaResponse<IncidenteResponse>> listar(@RequestParam(defaultValue="0") @Min(0) int pagina,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int tamanio, Principal principal) {
        return ResponseEntity.ok(service.listar(actor(principal),gestor(),pagina,tamanio));
    }
    @GetMapping("/{id}")
    public ResponseEntity<IncidenteResponse> obtener(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(service.obtener(id,actor(principal),gestor()));
    }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<IncidenteResponse> cambiarEstado(@PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoIncidenteRequest request) {
        return ResponseEntity.ok(service.cambiarEstado(id,request.estado()));
    }
    private UUID actor(Principal principal) { return UUID.fromString(principal.getName()); }
    private boolean gestor() { var auth=SecurityContextHolder.getContext().getAuthentication(); return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("INCIDENTE_GESTIONAR")); }
}
