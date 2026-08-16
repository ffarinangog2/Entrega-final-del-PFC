package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.equipo.EquipoEstadoRequest;
import ec.edu.scli.academico.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.service.EquipoService;
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
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @PostMapping("/api/v1/equipos")
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest request) {

        EquipoResponse creado = equipoService.crear(request);

        URI ubicacion = URI.create("/api/v1/equipos/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/api/v1/equipos")
    public ResponseEntity<Page<EquipoResponse>> listar(
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) EstadoEquipo estado,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(equipoService.listar(laboratorioId, estado, activo, pageable));
    }

    @GetMapping("/api/v1/equipos/{id}")
    public ResponseEntity<EquipoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(equipoService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/laboratorios/{laboratorioId}/equipos")
    public ResponseEntity<List<EquipoResponse>> listarPorLaboratorio(@PathVariable UUID laboratorioId) {
        return ResponseEntity.ok(equipoService.listarPorLaboratorio(laboratorioId));
    }

    @PutMapping("/api/v1/equipos/{id}")
    public ResponseEntity<EquipoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EquipoRequest request
    ) {
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    @PatchMapping("/api/v1/equipos/{id}/estado")
    public ResponseEntity<EquipoResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EquipoEstadoRequest request
    ) {
        return ResponseEntity.ok(equipoService.cambiarEstado(id, request.estado()));
    }
}
