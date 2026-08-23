package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.FacultadService;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadEstadoRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadResponse;
import jakarta.validation.Valid;

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
