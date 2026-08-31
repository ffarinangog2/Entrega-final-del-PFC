package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilEstadoRequest;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilUpdateRequest;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilMeUpdateRequest;
import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.security.JwtPrincipal;

import jakarta.validation.Valid;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfiles")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilResponse> obtenerPropio(java.security.Principal principal) {
        JwtPrincipal identidad = identidad(principal);
        return ResponseEntity.ok(perfilService.obtenerPorId(identidad.perfilId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<PerfilResponse> actualizarPropio(
            java.security.Principal principal,
            @Valid @RequestBody PerfilMeUpdateRequest request) {
        JwtPrincipal identidad = identidad(principal);
        return ResponseEntity.ok(perfilService.actualizarPropio(identidad.perfilId(), request));
    }

    private JwtPrincipal identidad(java.security.Principal principal) {
        if (principal instanceof JwtPrincipal jwtPrincipal) return jwtPrincipal;
        if (principal instanceof org.springframework.security.core.Authentication authentication
                && authentication.getPrincipal() instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "No se pudo resolver la identidad autenticada");
    }

    @PostMapping
    public ResponseEntity<PerfilResponse> crear(
            @Valid @RequestBody PerfilCreateRequest request
    ) {

        PerfilResponse perfilCreado = perfilService.crear(request);

        URI ubicacion = URI.create(
                "/api/v1/perfiles/" + perfilCreado.id()
        );

        return ResponseEntity
                .created(ubicacion)
                .body(perfilCreado);
    }

//    @GetMapping
//    public ResponseEntity<Page<PerfilResponse>> listar(
//            Pageable pageable
//    ) {
//
//        Page<PerfilResponse> perfiles =
//                perfilService.listar(pageable);
//
//        return ResponseEntity.ok(perfiles);
//    }

    @GetMapping
    public ResponseEntity<Page<PerfilResponse>> listar(

            @RequestParam(required = false)
            String identificacion,

            @RequestParam(required = false)
            String nombre,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            TipoPerfil tipoPerfil,

            @RequestParam(required = false)
            Boolean activo,

            @ParameterObject Pageable pageable
    ) {

        Page<PerfilResponse> perfiles =
                perfilService.listar(
                        identificacion,
                        nombre,
                        email,
                        tipoPerfil,
                        activo,
                        pageable
                );

        return ResponseEntity.ok(perfiles);
    }



    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponse> obtenerPorId(
            @PathVariable UUID id
    ) {

        PerfilResponse perfil =
                perfilService.obtenerPorId(id);

        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerfilResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PerfilUpdateRequest request
    ) {

        PerfilResponse perfilActualizado =
                perfilService.actualizar(id, request);

        return ResponseEntity.ok(perfilActualizado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PerfilResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody PerfilEstadoRequest request
    ) {

        PerfilResponse perfilActualizado =
                perfilService.cambiarEstado(
                        id,
                        request.activo()
                );

        return ResponseEntity.ok(perfilActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id
    ) {

        perfilService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
