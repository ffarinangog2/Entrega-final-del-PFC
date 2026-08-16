
package ec.edu.scli.reservas.controller;

import ec.edu.scli.reservas.dto.request.CancelarReservaRequest;
import ec.edu.scli.reservas.dto.response.PaginaResponse;
import ec.edu.scli.reservas.dto.response.ReservaResponse;
import ec.edu.scli.reservas.enums.EstadoReserva;
import ec.edu.scli.reservas.service.ReservaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/** Expone las operaciones REST para consultar y administrar reservas. */
@RestController
@Validated
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<ReservaResponse>> listar(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) UUID responsableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(reservaService.listar(
                estado, laboratorioId, responsableId, fechaDesde, fechaHasta, pagina, tamanio));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.buscarPorId(id));
    }

    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<PaginaResponse<ReservaResponse>> listarPorLaboratorio(
            @PathVariable UUID laboratorioId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(reservaService.listarPorLaboratorio(laboratorioId, pagina, tamanio));
    }

    @GetMapping("/responsable/{responsableId}")
    public ResponseEntity<PaginaResponse<ReservaResponse>> listarPorResponsable(
            @PathVariable UUID responsableId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(reservaService.listarPorResponsable(responsableId, pagina, tamanio));
    }

    @GetMapping("/calendario")
    public ResponseEntity<PaginaResponse<ReservaResponse>> obtenerCalendario(
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio) {
        return ResponseEntity.ok(reservaService.obtenerCalendario(
                laboratorioId, fechaDesde, fechaHasta, pagina, tamanio));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable UUID id, @Valid @RequestBody CancelarReservaRequest request,
            Principal principal) {
        return ResponseEntity.ok(reservaService.cancelar(id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<ReservaResponse> iniciar(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(reservaService.iniciar(id, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ReservaResponse> finalizar(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(reservaService.finalizar(id, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/no-asistida")
    public ResponseEntity<ReservaResponse> marcarNoAsistida(
            @PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(
                reservaService.marcarNoAsistida(id, obtenerUsuarioId(principal)));
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
