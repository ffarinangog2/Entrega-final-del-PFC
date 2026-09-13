package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.service.UsuarioInstitucionalAdminService;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.presentation.dto.usuarios.UsuarioInstitucionalCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.usuarios.UsuarioInstitucionalUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfiles/administracion-usuarios")
public class UsuarioInstitucionalAdminController {
    private final UsuarioInstitucionalAdminService service;

    public UsuarioInstitucionalAdminController(UsuarioInstitucionalAdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse crear(@Valid @RequestBody UsuarioInstitucionalCreateRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{perfilId}")
    public PerfilResponse actualizar(@PathVariable UUID perfilId,
            @Valid @RequestBody UsuarioInstitucionalUpdateRequest request) {
        return service.actualizar(perfilId, request);
    }
}
