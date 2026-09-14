package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.PlanificacionService;
import ec.edu.scli.reservas.presentation.dto.request.GuardarPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ObservacionPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planificaciones")
public class PlanificacionController {
    private final PlanificacionService service;

    public PlanificacionController(PlanificacionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PlanificacionResponse> crear(@Valid @RequestBody GuardarPlanificacionRequest request) {
        PlanificacionResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/planificaciones/" + response.id())).body(response);
    }

    @GetMapping public List<PlanificacionResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public PlanificacionResponse buscar(@PathVariable UUID id) { return service.buscar(id); }
    @PatchMapping("/{id}") public PlanificacionResponse editar(@PathVariable UUID id,
            @Valid @RequestBody GuardarPlanificacionRequest request) { return service.editar(id, request); }
    @PostMapping("/{id}/enviar") public PlanificacionResponse enviar(@PathVariable UUID id) { return service.enviar(id); }
    @PostMapping("/{id}/aceptar") public PlanificacionResponse aceptar(@PathVariable UUID id) { return service.aceptar(id); }
    @PostMapping("/{id}/rechazar") public PlanificacionResponse rechazar(@PathVariable UUID id,
            @RequestBody(required = false) ObservacionPlanificacionRequest request) {
        return service.rechazar(id, request == null ? null : request.observacion());
    }
    @PostMapping("/{id}/proponer-alternativa") public PlanificacionResponse proponer(@PathVariable UUID id,
            @RequestBody ProponerPlanificacionRequest request) { return service.proponer(id, request); }
    @PostMapping("/{id}/aceptar-propuesta") public PlanificacionResponse aceptarPropuesta(@PathVariable UUID id) {
        return service.aceptarPropuesta(id);
    }
    @PostMapping("/{id}/reenviar") public PlanificacionResponse reenviar(@PathVariable UUID id) { return service.reenviar(id); }
    @PostMapping("/{id}/cancelar") public PlanificacionResponse cancelar(@PathVariable UUID id) { return service.cancelar(id); }
}
