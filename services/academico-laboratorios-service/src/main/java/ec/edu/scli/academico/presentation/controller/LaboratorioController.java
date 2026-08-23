package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.List;
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

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioEstadoRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/laboratorios")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    public LaboratorioController(LaboratorioService laboratorioService) {
        this.laboratorioService = laboratorioService;
    }

    @PostMapping
    public ResponseEntity<LaboratorioResponse> crear(@Valid @RequestBody LaboratorioRequest request) {

        LaboratorioResponse creado = laboratorioService.crear(request);

        URI ubicacion = URI.create("/api/v1/laboratorios/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<Page<LaboratorioResponse>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoLaboratorio estado,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(laboratorioService.listar(texto, estado, activo, pageable));
    }

    // Nota: se registra ANTES que "/{id}" para que Spring no interprete
    // "disponibles" como si fuera un UUID de path variable.
    @GetMapping("/disponibles")
    public ResponseEntity<List<LaboratorioResponse>> listarDisponibles() {
        return ResponseEntity.ok(laboratorioService.listarDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaboratorioResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(laboratorioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaboratorioResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody LaboratorioRequest request
    ) {
        return ResponseEntity.ok(laboratorioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<LaboratorioResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody LaboratorioEstadoRequest request
    ) {
        return ResponseEntity.ok(laboratorioService.cambiarEstado(id, request.estado()));
    }
}
