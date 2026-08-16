package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.piso.PisoRequest;
import ec.edu.scli.academico.dto.piso.PisoResponse;
import ec.edu.scli.academico.service.PisoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
public class PisoController {

    private final PisoService pisoService;

    public PisoController(PisoService pisoService) {
        this.pisoService = pisoService;
    }

    @PostMapping("/api/v1/pisos")
    public ResponseEntity<PisoResponse> crear(@Valid @RequestBody PisoRequest request) {

        PisoResponse creado = pisoService.crear(request);

        URI ubicacion = URI.create("/api/v1/pisos/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/api/v1/pisos")
    public ResponseEntity<Page<PisoResponse>> listar(
            @RequestParam(required = false) UUID bloqueId,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(pisoService.listar(bloqueId, activo, pageable));
    }

    @GetMapping("/api/v1/pisos/{id}")
    public ResponseEntity<PisoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pisoService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/bloques/{bloqueId}/pisos")
    public ResponseEntity<List<PisoResponse>> listarPorBloque(@PathVariable UUID bloqueId) {
        return ResponseEntity.ok(pisoService.listarPorBloque(bloqueId));
    }

    @PutMapping("/api/v1/pisos/{id}")
    public ResponseEntity<PisoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PisoRequest request
    ) {
        return ResponseEntity.ok(pisoService.actualizar(id, request));
    }

    @DeleteMapping("/api/v1/pisos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        pisoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
