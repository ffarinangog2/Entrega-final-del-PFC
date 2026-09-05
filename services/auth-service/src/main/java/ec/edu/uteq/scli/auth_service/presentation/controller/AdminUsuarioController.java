package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.application.service.AdminUsuarioService;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioCreateRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/admin/usuarios")
public class AdminUsuarioController {
    private final AdminUsuarioService service;

    public AdminUsuarioController(AdminUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUsuarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<AdminUsuarioResponse> crear(@Valid @RequestBody AdminUsuarioCreateRequest request) {
        AdminUsuarioResponse creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/auth/admin/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    public AdminUsuarioResponse actualizar(@PathVariable UUID id,
            @Valid @RequestBody AdminUsuarioUpdateRequest request) {
        return service.actualizar(id, request);
    }
}
