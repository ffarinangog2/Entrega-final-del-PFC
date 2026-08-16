package ec.edu.scli.usuarios.controller;

import ec.edu.scli.usuarios.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.dto.administrador.AdministradorResponse;
import ec.edu.scli.usuarios.service.AdministradorService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/administradores")
public class AdministradorController {

    private final AdministradorService administradorService;

    public AdministradorController(
            AdministradorService administradorService
    ) {
        this.administradorService = administradorService;
    }

    @PostMapping
    public ResponseEntity<AdministradorResponse> crear(
            @Valid @RequestBody AdministradorRequest request
    ) {

        AdministradorResponse administradorCreado =
                administradorService.crear(request);

        URI ubicacion = URI.create(
                "/api/v1/administradores/"
                        + administradorCreado.id()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(administradorCreado);
    }

    @GetMapping
    public ResponseEntity<Page<AdministradorResponse>> listar(
            Pageable pageable
    ) {

        Page<AdministradorResponse> administradores =
                administradorService.listar(pageable);

        return ResponseEntity.ok(administradores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministradorResponse> obtenerPorId(
            @PathVariable UUID id
    ) {

        AdministradorResponse administrador =
                administradorService.obtenerPorId(id);

        return ResponseEntity.ok(administrador);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdministradorResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AdministradorRequest request
    ) {

        AdministradorResponse administradorActualizado =
                administradorService.actualizar(id, request);

        return ResponseEntity.ok(administradorActualizado);
    }
}