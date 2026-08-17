package ec.edu.scli.reservas.controller;

import ec.edu.scli.reservas.dto.request.CrearBloqueoAgendaRequest;
import ec.edu.scli.reservas.dto.response.AgendaItemResponse;
import ec.edu.scli.reservas.dto.response.BloqueoAgendaResponse;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.service.AgendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/** Expone las operaciones REST de consulta y bloqueo de agenda. */
@RestController
@Validated
@RequestMapping("/api/v1/agenda")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<AgendaItemResponse>> listar(
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(agendaService.listar(laboratorioId, fechaDesde, fechaHasta, pagina, tamanio));
    }

    @GetMapping("/laboratorios/{laboratorioId}")
    public ResponseEntity<PaginaResponse<AgendaItemResponse>> listarPorLaboratorio(
            @PathVariable UUID laboratorioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(agendaService.listarPorLaboratorio(
                laboratorioId, fechaDesde, fechaHasta, pagina, tamanio));
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<BloqueoAgendaResponse> crearBloqueo(
            @Valid @RequestBody CrearBloqueoAgendaRequest request, Principal principal) {
        BloqueoAgendaResponse respuesta = agendaService.crearBloqueo(request, obtenerUsuarioId(principal));
        return ResponseEntity.created(URI.create("/api/v1/agenda/bloqueos/" + respuesta.id())).body(respuesta);
    }

    @DeleteMapping("/bloqueos/{id}")
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable UUID id, Principal principal) {
        agendaService.eliminarBloqueo(id, obtenerUsuarioId(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID obtenerUsuarioId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No existe un usuario autenticado");
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("La identidad autenticada no contiene un UUID válido");
        }
    }
}
