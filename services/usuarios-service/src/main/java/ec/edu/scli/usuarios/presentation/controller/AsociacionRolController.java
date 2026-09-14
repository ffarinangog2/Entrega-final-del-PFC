package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.service.AsociacionRolService;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AsociacionRolRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AsociacionRolResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfiles/{perfilId}/asociacion-rol")
public class AsociacionRolController {
    private final AsociacionRolService asociaciones;

    public AsociacionRolController(AsociacionRolService asociaciones) {
        this.asociaciones = asociaciones;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public AsociacionRolResponse consultar(@PathVariable UUID perfilId) {
        return asociaciones.consultar(perfilId);
    }

    @PutMapping
    @Transactional
    public ResponseEntity<Void> asociar(@PathVariable UUID perfilId,
            @Valid @RequestBody AsociacionRolRequest request) {
        asociaciones.asociar(perfilId, request);
        return ResponseEntity.noContent().build();
    }
}
