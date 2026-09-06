package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.SolicitudRetiroPlanificacionService;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudRetiroRequest;
import ec.edu.scli.reservas.presentation.dto.request.ResolverSolicitudRetiroRequest;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudRetiroResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/planificaciones-agregadas/{planificacionId}/solicitudes-retiro")
public class SolicitudRetiroPlanificacionController {
    private final SolicitudRetiroPlanificacionService service;
    public SolicitudRetiroPlanificacionController(SolicitudRetiroPlanificacionService service) {
        this.service = service;
    }
    @GetMapping
    public List<SolicitudRetiroResponse> listar(@PathVariable UUID planificacionId) {
        return service.listar(planificacionId);
    }
    @PostMapping
    public SolicitudRetiroResponse crear(@PathVariable UUID planificacionId,
            @Valid @RequestBody CrearSolicitudRetiroRequest request) {
        return service.crear(planificacionId, request.motivo());
    }
    @PostMapping("/{solicitudId}/aprobar")
    public SolicitudRetiroResponse aprobar(@PathVariable UUID planificacionId,
            @PathVariable UUID solicitudId, @RequestBody(required = false) ResolverSolicitudRetiroRequest request) {
        return service.aprobar(planificacionId, solicitudId, request == null ? null : request.observacion());
    }
    @PostMapping("/{solicitudId}/rechazar")
    public SolicitudRetiroResponse rechazar(@PathVariable UUID planificacionId,
            @PathVariable UUID solicitudId, @RequestBody(required = false) ResolverSolicitudRetiroRequest request) {
        return service.rechazar(planificacionId, solicitudId, request == null ? null : request.observacion());
    }
}
