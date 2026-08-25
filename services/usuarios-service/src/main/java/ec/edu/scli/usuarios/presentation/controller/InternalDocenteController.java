package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.DocenteService;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteInternoResponse;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/docentes")
public class InternalDocenteController {
    private final DocenteService docentes;
    private final String internalApiKey;

    public InternalDocenteController(DocenteService docentes,
                                     @Value("${app.internal-api-key}") String internalApiKey) {
        this.docentes = docentes;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/{docenteId}")
    public ResponseEntity<DocenteInternoResponse> obtenerPorId(
            @PathVariable UUID docenteId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!claveValida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(respuesta(docentes.obtenerPorId(docenteId)));
    }

    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<DocenteInternoResponse> obtenerPorPerfilId(
            @PathVariable UUID perfilId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!claveValida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(respuesta(docentes.obtenerPorPerfilId(perfilId)));
    }

    private boolean claveValida(String value) {
        return value != null && !value.isBlank() && internalApiKey.equals(value);
    }

    private DocenteInternoResponse respuesta(DocenteResponse docente) {
        return new DocenteInternoResponse(docente.id(), docente.perfilId(), Boolean.TRUE.equals(docente.activo()));
    }
}
