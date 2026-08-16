package ec.edu.scli.usuarios.controller;

import ec.edu.scli.usuarios.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.dto.estudiante.EstudianteResponse;
import ec.edu.scli.usuarios.service.EstudianteService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @PostMapping
    public ResponseEntity<EstudianteResponse> crear(
            @Valid @RequestBody EstudianteRequest request
    ) {

        EstudianteResponse estudianteCreado =
                estudianteService.crear(request);

        URI ubicacion = URI.create(
                "/api/v1/estudiantes/" + estudianteCreado.id()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(estudianteCreado);
    }

    @GetMapping
    public ResponseEntity<Page<EstudianteResponse>> listar(
            Pageable pageable
    ) {

        Page<EstudianteResponse> estudiantes =
                estudianteService.listar(pageable);

        return ResponseEntity.ok(estudiantes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstudianteResponse> obtenerPorId(
            @PathVariable UUID id
    ) {

        EstudianteResponse estudiante =
                estudianteService.obtenerPorId(id);

        return ResponseEntity.ok(estudiante);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstudianteResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EstudianteRequest request
    ) {

        EstudianteResponse estudianteActualizado =
                estudianteService.actualizar(id, request);

        return ResponseEntity.ok(estudianteActualizado);
    }
}