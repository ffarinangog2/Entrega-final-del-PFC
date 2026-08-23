package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.CampusService;
import ec.edu.scli.academico.presentation.dto.campus.CampusRequest;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/campus")
public class CampusController {

    private final CampusService campusService;

    public CampusController(CampusService campusService) {
        this.campusService = campusService;
    }

    @PostMapping
    public ResponseEntity<CampusResponse> crear(@Valid @RequestBody CampusRequest request) {

        CampusResponse creado = campusService.crear(request);

        URI ubicacion = URI.create("/api/v1/campus/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<Page<CampusResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(campusService.listar(codigo, nombre, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampusResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(campusService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampusResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CampusRequest request
    ) {
        return ResponseEntity.ok(campusService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        campusService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
