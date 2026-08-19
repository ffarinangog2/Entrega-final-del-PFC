package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.service.BloqueService;
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
public class BloqueController {

    private final BloqueService bloqueService;

    public BloqueController(BloqueService bloqueService) {
        this.bloqueService = bloqueService;
    }

    @PostMapping("/api/v1/bloques")
    public ResponseEntity<BloqueResponse> crear(@Valid @RequestBody BloqueRequest request) {

        BloqueResponse creado = bloqueService.crear(request);

        URI ubicacion = URI.create("/api/v1/bloques/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/api/v1/bloques")
    public ResponseEntity<Page<BloqueResponse>> listar(
            @RequestParam(required = false) UUID campusId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(bloqueService.listar(campusId, nombre, activo, pageable));
    }

    @GetMapping("/api/v1/bloques/{id}")
    public ResponseEntity<BloqueResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(bloqueService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/campus/{campusId}/bloques")
    public ResponseEntity<List<BloqueResponse>> listarPorCampus(@PathVariable UUID campusId) {
        return ResponseEntity.ok(bloqueService.listarPorCampus(campusId));
    }

    @PutMapping("/api/v1/bloques/{id}")
    public ResponseEntity<BloqueResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody BloqueRequest request
    ) {
        return ResponseEntity.ok(bloqueService.actualizar(id, request));
    }

    @DeleteMapping("/api/v1/bloques/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        bloqueService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
