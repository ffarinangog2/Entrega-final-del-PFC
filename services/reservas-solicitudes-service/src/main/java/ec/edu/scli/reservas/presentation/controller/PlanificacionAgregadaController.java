package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.PlanificacionAgregadaService;
import ec.edu.scli.reservas.presentation.dto.request.IniciarPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ObservacionPlanificacionRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerCambioAgregadoRequest;
import ec.edu.scli.reservas.presentation.dto.response.PlanificacionAgregadaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planificaciones-agregadas")
public class PlanificacionAgregadaController {
    private final PlanificacionAgregadaService service;

    public PlanificacionAgregadaController(PlanificacionAgregadaService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PlanificacionAgregadaResponse> iniciar(
            @Valid @RequestBody IniciarPlanificacionRequest request) {
        PlanificacionAgregadaResponse response = service.iniciar(request.periodoId());
        return ResponseEntity.created(URI.create("/api/v1/planificaciones-agregadas/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<PlanificacionAgregadaResponse> listar() { return service.listar(); }

    @PostMapping("/{id}/enviar")
    public PlanificacionAgregadaResponse enviar(@PathVariable UUID id) { return service.enviar(id); }

    @PostMapping("/{id}/revisiones/mi-piso/aprobar")
    public PlanificacionAgregadaResponse aprobarPiso(@PathVariable UUID id) { return service.aprobarPiso(id); }

    @PostMapping("/{id}/revisiones/mi-piso/rechazar")
    public PlanificacionAgregadaResponse rechazarPiso(@PathVariable UUID id,
            @RequestBody ObservacionPlanificacionRequest request) {
        return service.rechazarPiso(id, request.observacion());
    }

    @PostMapping("/{id}/revisiones/mi-piso/proponer-cambio")
    public PlanificacionAgregadaResponse proponerCambio(@PathVariable UUID id,
            @Valid @RequestBody ProponerCambioAgregadoRequest request) {
        return service.proponerCambio(id, request);
    }
}
