package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.facultad.FacultadEstadoRequest;
import ec.edu.scli.academico.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.dto.facultad.FacultadResponse;
import ec.edu.scli.academico.service.FacultadService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facultades")
public class FacultadController {

    private final FacultadService facultadService;

    public FacultadController(FacultadService facultadService) {
        this.facultadService = facultadService;
    }

    @PostMapping
    public ResponseEntity<FacultadResponse> crear(@Valid @RequestBody FacultadRequest request) {

        FacultadResponse creada = facultadService.crear(request);

        URI ubicacion = URI.create("/api/v1/facultades/" + creada.id());

        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public ResponseEntity<Page<FacultadResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(facultadService.listar(codigo, nombre, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultadResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(facultadService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacultadResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody FacultadRequest request
    ) {
        return ResponseEntity.ok(facultadService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<FacultadResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody FacultadEstadoRequest request
    ) {
        return ResponseEntity.ok(facultadService.cambiarEstado(id, request.activo()));
    }
}
