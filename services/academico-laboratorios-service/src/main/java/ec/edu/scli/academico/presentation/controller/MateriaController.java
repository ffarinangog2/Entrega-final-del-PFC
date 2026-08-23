package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.presentation.dto.materia.MateriaRequest;
import ec.edu.scli.academico.presentation.dto.materia.MateriaResponse;
import jakarta.validation.Valid;

@RestController
public class MateriaController {

    private final MateriaService materiaService;

    public MateriaController(MateriaService materiaService) {
        this.materiaService = materiaService;
    }

    @PostMapping("/api/v1/materias")
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {

        MateriaResponse creada = materiaService.crear(request);

        URI ubicacion = URI.create("/api/v1/materias/" + creada.id());

        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping("/api/v1/materias")
    public ResponseEntity<Page<MateriaResponse>> listar(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(materiaService.listar(carreraId, codigo, nombre, activo, pageable));
    }

    @GetMapping("/api/v1/materias/{id}")
    public ResponseEntity<MateriaResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(materiaService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/carreras/{carreraId}/materias")
    public ResponseEntity<List<MateriaResponse>> listarPorCarrera(@PathVariable UUID carreraId) {
        return ResponseEntity.ok(materiaService.listarPorCarrera(carreraId));
    }

    @PutMapping("/api/v1/materias/{id}")
    public ResponseEntity<MateriaResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MateriaRequest request
    ) {
        return ResponseEntity.ok(materiaService.actualizar(id, request));
    }

    @DeleteMapping("/api/v1/materias/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        materiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
