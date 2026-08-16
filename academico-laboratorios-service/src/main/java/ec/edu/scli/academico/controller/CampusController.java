package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.campus.CampusRequest;
import ec.edu.scli.academico.dto.campus.CampusResponse;
import ec.edu.scli.academico.service.CampusService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

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
