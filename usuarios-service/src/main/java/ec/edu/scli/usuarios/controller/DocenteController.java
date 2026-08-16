package ec.edu.scli.usuarios.controller;

import ec.edu.scli.usuarios.dto.docente.DocenteRequest;
import ec.edu.scli.usuarios.dto.docente.DocenteResponse;
import ec.edu.scli.usuarios.service.DocenteService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @PostMapping
    public ResponseEntity<DocenteResponse> crear(
            @Valid @RequestBody DocenteRequest request
    ) {

        DocenteResponse docenteCreado =
                docenteService.crear(request);

        URI ubicacion = URI.create(
                "/api/v1/docentes/" + docenteCreado.id()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(docenteCreado);
    }

    @GetMapping
    public ResponseEntity<Page<DocenteResponse>> listar(
            Pageable pageable
    ) {

        Page<DocenteResponse> docentes =
                docenteService.listar(pageable);

        return ResponseEntity.ok(docentes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocenteResponse> obtenerPorId(
            @PathVariable UUID id
    ) {

        DocenteResponse docente =
                docenteService.obtenerPorId(id);

        return ResponseEntity.ok(docente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocenteResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody DocenteRequest request
    ) {

        DocenteResponse docenteActualizado =
                docenteService.actualizar(id, request);

        return ResponseEntity.ok(docenteActualizado);
    }

    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<DocenteResponse> obtenerPorPerfilId(
            @PathVariable UUID perfilId
    ) {

        DocenteResponse docente =
                docenteService.obtenerPorPerfilId(perfilId);

        return ResponseEntity.ok(docente);
    }
}