package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.EstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteInternoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/estudiantes")
public class InternalEstudianteController {
    private final EstudianteService estudiantes;
    private final String internalApiKey;
    public InternalEstudianteController(EstudianteService estudiantes,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.estudiantes = estudiantes; this.internalApiKey = internalApiKey;
    }
    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<EstudianteInternoResponse> obtenerPorPerfil(@PathVariable UUID perfilId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key) {
        if (key == null || !internalApiKey.equals(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var e = estudiantes.obtenerPorPerfilId(perfilId);
        return ResponseEntity.ok(new EstudianteInternoResponse(e.id(), e.perfilId(), e.carreraId(), Boolean.TRUE.equals(e.activo())));
    }
}
