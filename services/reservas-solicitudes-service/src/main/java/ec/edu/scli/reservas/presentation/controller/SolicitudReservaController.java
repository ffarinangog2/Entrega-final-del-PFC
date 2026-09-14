package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.presentation.dto.request.ActualizarSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.AprobarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CancelarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.CrearSolicitudReservaRequest;
import ec.edu.scli.reservas.presentation.dto.request.RechazarSolicitudRequest;
import ec.edu.scli.reservas.presentation.dto.request.ProponerAlternativaRequest;
import ec.edu.scli.reservas.presentation.dto.request.ResponderPropuestaRequest;
import ec.edu.scli.reservas.presentation.dto.response.HistorialSolicitudResponse;
import ec.edu.scli.reservas.presentation.dto.response.PaginaResponse;
import ec.edu.scli.reservas.presentation.dto.response.ReservaResponse;
import ec.edu.scli.reservas.presentation.dto.response.SolicitudReservaResponse;
import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.application.service.SolicitudReservaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

/** Expone las operaciones REST para administrar solicitudes de reserva. */
@RestController
@Validated
@RequestMapping("/api/v1/solicitudes")
public class SolicitudReservaController {

    private final SolicitudReservaService solicitudReservaService;

    public SolicitudReservaController(SolicitudReservaService solicitudReservaService) {
        this.solicitudReservaService = solicitudReservaService;
    }

    @PostMapping
    public ResponseEntity<SolicitudReservaResponse> crear(
            @Valid @RequestBody CrearSolicitudReservaRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String claveIdempotencia,
            Principal principal) {
        UUID perfilAutenticadoId = obtenerUsuarioId(principal);
        CrearSolicitudReservaRequest requestAutenticado =
                asociarSolicitanteAutenticado(request, perfilAutenticadoId);
        SolicitudReservaResponse respuesta = solicitudReservaService.crear(
                requestAutenticado, claveIdempotencia, perfilAutenticadoId);
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + respuesta.id())).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<SolicitudReservaResponse>> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) UUID solicitanteId,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            Authentication authentication) {
        UUID actorId = obtenerUsuarioId(authentication);
        return ResponseEntity.ok(solicitudReservaService.listarAutorizado(
                estado, solicitanteId, laboratorioId, fecha, pagina, tamanio, actorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudReservaResponse> buscarPorId(
            @PathVariable UUID id, Authentication authentication) {
        SolicitudReservaResponse respuesta = solicitudReservaService
                .buscarPorIdAutorizado(id, obtenerUsuarioId(authentication));
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/solicitante/{solicitanteId}")
    public ResponseEntity<PaginaResponse<SolicitudReservaResponse>> listarPorSolicitante(
            @PathVariable UUID solicitanteId,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudReservaService.listarAutorizado(
                null, solicitanteId, null, null, pagina, tamanio,
                obtenerUsuarioId(authentication)));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<PaginaResponse<SolicitudReservaResponse>> listarPorEstado(
            @PathVariable EstadoSolicitud estado,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            Authentication authentication) {
        return ResponseEntity.ok(solicitudReservaService.listarAutorizado(
                estado, null, null, null, pagina, tamanio,
                obtenerUsuarioId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitudReservaResponse> actualizar(
            @PathVariable UUID id, @Valid @RequestBody ActualizarSolicitudReservaRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.actualizar(id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/revision")
    public ResponseEntity<SolicitudReservaResponse> ponerEnRevision(
            @PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.ponerEnRevision(id, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ReservaResponse> aprobar(
            @PathVariable UUID id, @Valid @RequestBody AprobarSolicitudRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String claveIdempotencia,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.aprobar(
                id, request, claveIdempotencia, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<SolicitudReservaResponse> rechazar(
            @PathVariable UUID id, @Valid @RequestBody RechazarSolicitudRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.rechazar(id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<SolicitudReservaResponse> cancelar(
            @PathVariable UUID id, @Valid @RequestBody CancelarSolicitudRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.cancelar(id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/propuesta")
    public ResponseEntity<SolicitudReservaResponse> proponerAlternativa(
            @PathVariable UUID id, @Valid @RequestBody ProponerAlternativaRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.proponerAlternativa(
                id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/propuesta/aceptar")
    public ResponseEntity<SolicitudReservaResponse> aceptarPropuesta(
            @PathVariable UUID id, @Valid @RequestBody ResponderPropuestaRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.aceptarPropuesta(
                id, request, obtenerUsuarioId(principal)));
    }

    @PostMapping("/{id}/propuesta/rechazar")
    public ResponseEntity<SolicitudReservaResponse> rechazarPropuesta(
            @PathVariable UUID id, @Valid @RequestBody ResponderPropuestaRequest request,
            Principal principal) {
        return ResponseEntity.ok(solicitudReservaService.rechazarPropuesta(
                id, request, obtenerUsuarioId(principal)));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<PaginaResponse<HistorialSolicitudResponse>> obtenerHistorial(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio,
            Authentication authentication) {
        solicitudReservaService.buscarPorIdAutorizado(id, obtenerUsuarioId(authentication));
        return ResponseEntity.ok(solicitudReservaService.obtenerHistorial(id, pagina, tamanio));
    }

    private void validarLecturaPropia(UUID solicitanteId, Authentication authentication) {
        if (!puedeGestionarSolicitudes(authentication)
                && !solicitanteId.equals(obtenerUsuarioId(authentication))) {
            throw new AccessDeniedException("No puede consultar solicitudes de otro usuario");
        }
    }

    private boolean puedeGestionarSolicitudes(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> authority.equals("SOLICITUD_APROBAR")
                        || authority.equals("SOLICITUD_RECHAZAR"));
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

    private CrearSolicitudReservaRequest asociarSolicitanteAutenticado(
            CrearSolicitudReservaRequest request, UUID perfilAutenticadoId) {
        return new CrearSolicitudReservaRequest(
                perfilAutenticadoId,
                request.docenteId(),
                request.laboratorioId(),
                request.materiaId(),
                request.periodoLectivoId(),
                request.fechaReserva(),
                request.horaInicio(),
                request.horaFin(),
                request.numeroParticipantes(),
                request.motivo(),
                request.observacion());
    }
}
