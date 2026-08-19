package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoResponse;
import ec.edu.scli.academico.service.PeriodoLectivoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/periodos-lectivos")
public class PeriodoLectivoController {

    private final PeriodoLectivoService periodoLectivoService;

    public PeriodoLectivoController(PeriodoLectivoService periodoLectivoService) {
        this.periodoLectivoService = periodoLectivoService;
    }

    @PostMapping
    public ResponseEntity<PeriodoLectivoResponse> crear(@Valid @RequestBody PeriodoLectivoRequest request) {

        PeriodoLectivoResponse creado = periodoLectivoService.crear(request);

        URI ubicacion = URI.create("/api/v1/periodos-lectivos/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<Page<PeriodoLectivoResponse>> listar(
            @RequestParam(required = false) String codigo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(periodoLectivoService.listar(codigo, pageable));
    }

    // Nota: se registra ANTES que "/{id}" para que Spring no interprete
    // "actual" como si fuera un UUID de path variable.
    @GetMapping("/actual")
    public ResponseEntity<PeriodoLectivoResponse> obtenerActual() {
        return ResponseEntity.ok(periodoLectivoService.obtenerActual());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodoLectivoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(periodoLectivoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeriodoLectivoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PeriodoLectivoRequest request
    ) {
        return ResponseEntity.ok(periodoLectivoService.actualizar(id, request));
    }
}
