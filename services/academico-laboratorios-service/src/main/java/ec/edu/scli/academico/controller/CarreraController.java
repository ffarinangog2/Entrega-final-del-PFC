package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.dto.carrera.CarreraResponse;
import ec.edu.scli.academico.service.CarreraService;
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
public class CarreraController {

    private final CarreraService carreraService;

    public CarreraController(CarreraService carreraService) {
        this.carreraService = carreraService;
    }

    @PostMapping("/api/v1/carreras")
    public ResponseEntity<CarreraResponse> crear(@Valid @RequestBody CarreraRequest request) {

        CarreraResponse creada = carreraService.crear(request);

        URI ubicacion = URI.create("/api/v1/carreras/" + creada.id());

        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/api/v1/carreras")
    public ResponseEntity<Page<CarreraResponse>> listar(
            @RequestParam(required = false) UUID facultadId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(carreraService.listar(facultadId, codigo, nombre, activo, pageable));
    }

    @GetMapping("/api/v1/carreras/{id}")
    public ResponseEntity<CarreraResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(carreraService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/facultades/{facultadId}/carreras")
    public ResponseEntity<List<CarreraResponse>> listarPorFacultad(@PathVariable UUID facultadId) {
        return ResponseEntity.ok(carreraService.listarPorFacultad(facultadId));
    }

    @PutMapping("/api/v1/carreras/{id}")
    public ResponseEntity<CarreraResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CarreraRequest request
    ) {
        return ResponseEntity.ok(carreraService.actualizar(id, request));
    }

    @DeleteMapping("/api/v1/carreras/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        carreraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
