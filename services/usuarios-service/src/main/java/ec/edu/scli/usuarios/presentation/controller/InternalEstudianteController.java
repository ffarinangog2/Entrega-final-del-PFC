package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.EstudianteService;
import ec.edu.scli.usuarios.application.service.ContextoAcademicoEstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteInternoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.ContextoAcademicoEstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;

@RestController
@RequestMapping("/api/v1/internal/estudiantes")
public class InternalEstudianteController {
    private final EstudianteService estudiantes;
    private final String internalApiKey;
    private final ContextoAcademicoEstudianteService contextos;
    private final ContextoAcademicoEstudianteRepository contextoRepository;
    private final EstudianteRepository estudianteRepository;
    public InternalEstudianteController(EstudianteService estudiantes,
            ContextoAcademicoEstudianteService contextos,
            @Value("${app.internal-api-key}") String internalApiKey,
            ContextoAcademicoEstudianteRepository contextoRepository, EstudianteRepository estudianteRepository) {
        this.estudiantes = estudiantes; this.contextos = contextos; this.internalApiKey = internalApiKey; this.contextoRepository=contextoRepository;this.estudianteRepository=estudianteRepository;
    }
    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<EstudianteInternoResponse> obtenerPorPerfil(@PathVariable UUID perfilId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key) {
        if (key == null || !internalApiKey.equals(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var e = estudiantes.obtenerPorPerfilId(perfilId);
        var contexto = contextos.actual(perfilId);
        return ResponseEntity.ok(new EstudianteInternoResponse(e.id(), e.perfilId(), contexto.carreraId(),
                contexto.periodoId(), contexto.nivel(), Boolean.TRUE.equals(e.activo())));
    }
    @GetMapping("/perfil/{perfilId}/periodo/{periodoId}")
    public ResponseEntity<EstudianteInternoResponse> obtenerContexto(@PathVariable UUID perfilId,@PathVariable UUID periodoId,
            @RequestHeader(value="X-Internal-Api-Key",required=false) String key) {
        if(key==null||!internalApiKey.equals(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var e=estudiantes.obtenerPorPerfilId(perfilId); var contexto=contextos.porPeriodo(perfilId,periodoId);
        return ResponseEntity.ok(new EstudianteInternoResponse(e.id(),e.perfilId(),contexto.carreraId(),contexto.periodoId(),contexto.nivel(),Boolean.TRUE.equals(e.activo())));
    }
    @GetMapping("/compatibles")
    public ResponseEntity<List<UUID>> compatibles(@RequestParam UUID carreraId,@RequestParam UUID periodoId,@RequestParam Integer nivel,
            @RequestHeader(value="X-Internal-Api-Key",required=false) String key){
        if(key==null||!internalApiKey.equals(key))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(contextoRepository.findByCarreraIdAndPeriodoIdAndNivelAndActivoTrue(carreraId,periodoId,nivel).stream()
                .map(c->estudianteRepository.findById(c.getEstudianteId()).orElse(null)).filter(java.util.Objects::nonNull)
                .filter(e->Boolean.TRUE.equals(e.getActivo())).map(e->e.getPerfil().getId()).distinct().toList());
    }
}
