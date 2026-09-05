package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.service.ContextoAcademicoEstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController @RequestMapping("/api/v1/estudiantes")
public class ContextoAcademicoEstudianteController {
    private final ContextoAcademicoEstudianteService service;
    public ContextoAcademicoEstudianteController(ContextoAcademicoEstudianteService service){this.service=service;}
    @GetMapping("/mi-contexto") public ContextoAcademicoEstudianteResponse actual(Principal principal){return service.actual(UUID.fromString(principal.getName()));}
    @GetMapping("/mis-contextos") public List<ContextoAcademicoEstudianteResponse> historial(Principal principal){return service.historial(UUID.fromString(principal.getName()));}
    @PostMapping("/mi-contexto") public ContextoAcademicoEstudianteResponse autodeclarar(Principal principal,@Valid @RequestBody ContextoAcademicoEstudianteRequest request){return service.autodeclarar(UUID.fromString(principal.getName()),request);}
    @PostMapping("/{estudianteId}/contextos") public ContextoAcademicoEstudianteResponse asignar(@PathVariable UUID estudianteId,@Valid @RequestBody ContextoAcademicoEstudianteRequest request){return service.asignar(estudianteId,request);}
    @PostMapping("/perfil/{perfilId}/contextos") public ContextoAcademicoEstudianteResponse asignarPerfil(@PathVariable UUID perfilId,@Valid @RequestBody ContextoAcademicoEstudianteRequest request){return service.asignarPorPerfil(perfilId,request);}
    @GetMapping("/perfil/{perfilId}/contextos") public List<ContextoAcademicoEstudianteResponse> historialPerfil(@PathVariable UUID perfilId){return service.historial(perfilId);}
}
