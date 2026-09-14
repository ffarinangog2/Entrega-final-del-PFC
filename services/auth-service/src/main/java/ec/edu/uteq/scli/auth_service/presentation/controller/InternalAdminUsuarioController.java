package ec.edu.uteq.scli.auth_service.presentation.controller;

import ec.edu.uteq.scli.auth_service.application.service.AdminUsuarioService;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioCreateRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/admin/usuarios")
public class InternalAdminUsuarioController {
    private final AdminUsuarioService service;
    private final String internalApiKey;

    public InternalAdminUsuarioController(AdminUsuarioService service,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.service = service;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping
    public AdminUsuarioResponse crear(@RequestHeader(value = "X-Internal-Api-Key", required = false) String key,
            @RequestBody AdminUsuarioCreateRequest request) {
        validar(key);
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public AdminUsuarioResponse actualizar(@PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key,
            @RequestBody AdminUsuarioUpdateRequest request) {
        validar(key);
        return service.actualizar(id, request);
    }

    @GetMapping("/{id}")
    public AdminUsuarioResponse obtener(@PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key) {
        validar(key);
        return service.obtener(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCredencialCreada(@PathVariable UUID id, @RequestParam UUID perfilId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key) {
        validar(key);
        service.eliminarCredencialCreada(id, perfilId);
    }

    private void validar(String key) {
        if (key == null || !internalApiKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna inválida.");
        }
    }
}
