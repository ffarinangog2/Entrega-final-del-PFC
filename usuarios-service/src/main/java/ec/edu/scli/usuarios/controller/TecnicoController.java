package ec.edu.scli.usuarios.controller;

import ec.edu.scli.usuarios.dto.tecnico.TecnicoRequest;
import ec.edu.scli.usuarios.dto.tecnico.TecnicoResponse;
import ec.edu.scli.usuarios.service.TecnicoService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @PostMapping
    public ResponseEntity<TecnicoResponse> crear(
            @Valid @RequestBody TecnicoRequest request
    ) {

        TecnicoResponse tecnicoCreado =
                tecnicoService.crear(request);

        URI ubicacion = URI.create(
                "/api/v1/tecnicos/" + tecnicoCreado.id()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(tecnicoCreado);
    }

    @GetMapping
    public ResponseEntity<Page<TecnicoResponse>> listar(
            Pageable pageable
    ) {

        Page<TecnicoResponse> tecnicos =
                tecnicoService.listar(pageable);

        return ResponseEntity.ok(tecnicos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TecnicoResponse> obtenerPorId(
            @PathVariable UUID id
    ) {

        TecnicoResponse tecnico =
                tecnicoService.obtenerPorId(id);

        return ResponseEntity.ok(tecnico);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TecnicoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TecnicoRequest request
    ) {

        TecnicoResponse tecnicoActualizado =
                tecnicoService.actualizar(id, request);

        return ResponseEntity.ok(tecnicoActualizado);
    }
}