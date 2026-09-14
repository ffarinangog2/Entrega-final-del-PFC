package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.service.ContextoAcademicoEstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoAcademicoEstudianteResponse;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoEstudianteResumenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/estudiantes")
public class ContextoAcademicoEstudianteController {
    private final ContextoAcademicoEstudianteService service;

    public ContextoAcademicoEstudianteController(ContextoAcademicoEstudianteService service) {
        this.service = service;
    }

    @GetMapping("/mi-contexto")
    public ContextoAcademicoEstudianteResponse actual(Principal principal) {
        return service.actual(UUID.fromString(principal.getName()));
    }

    @GetMapping("/mis-contextos")
    public List<ContextoAcademicoEstudianteResponse> historial(Principal principal) {
        return service.historial(UUID.fromString(principal.getName()));
    }

    @PostMapping("/mi-contexto")
    public ContextoAcademicoEstudianteResponse autodeclarar(
            Principal principal,
            @Valid @RequestBody ContextoAcademicoEstudianteRequest request) {
        return service.autodeclarar(UUID.fromString(principal.getName()), request);
    }

    @PostMapping("/{estudianteId}/contextos")
    public ContextoAcademicoEstudianteResponse asignar(
            @PathVariable UUID estudianteId,
            @Valid @RequestBody ContextoAcademicoEstudianteRequest request) {
        return service.asignar(estudianteId, request);
    }

    @PostMapping("/perfil/{perfilId}/contextos")
    public ContextoAcademicoEstudianteResponse asignarPerfil(
            @PathVariable UUID perfilId,
            @Valid @RequestBody ContextoAcademicoEstudianteRequest request) {
        return service.asignarPorPerfil(perfilId, request);
    }

    @GetMapping("/perfil/{perfilId}/contextos")
    public List<ContextoAcademicoEstudianteResponse> historialPerfil(@PathVariable UUID perfilId) {
        return service.historial(perfilId);
    }

    @GetMapping("/contextos")
    public List<ContextoEstudianteResumenResponse> listarContextosMasivos(
            @RequestParam(required = false) UUID periodoId) {
        return service.listarContextosMasivos(periodoId);
    }
}
