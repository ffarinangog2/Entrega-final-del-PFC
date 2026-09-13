package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.application.usecase.DocenteService;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteInternoResponse;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/docentes")
public class InternalDocenteController {
    private final DocenteService docentes;
    private final String internalApiKey;
    private final AdscripcionInstitucionalRepository adscripciones;

    @Autowired
    public InternalDocenteController(DocenteService docentes,
            @Value("${app.internal-api-key}") String internalApiKey,
            AdscripcionInstitucionalRepository adscripciones) {
        this.docentes = docentes;
        this.internalApiKey = internalApiKey;
        this.adscripciones = adscripciones;
    }

    InternalDocenteController(DocenteService docentes, String internalApiKey) {
        this(docentes, internalApiKey, null);
    }

    @GetMapping("/{docenteId}/carreras/{carreraId}/exists")
    public ResponseEntity<Boolean> perteneceCarrera(@PathVariable UUID docenteId,
            @PathVariable UUID carreraId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!claveValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DocenteResponse docente = docentes.obtenerPorId(docenteId);
        boolean pertenece = adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(docente.perfilId()).stream()
                .anyMatch(item -> item.isActivo()
                        && item.getTipoAmbito() == TipoAmbitoInstitucional.CARRERA
                        && carreraId.equals(item.getAmbitoId()));
        return ResponseEntity.ok(pertenece);
    }

    @GetMapping("/{docenteId}")
    public ResponseEntity<DocenteInternoResponse> obtenerPorId(
            @PathVariable UUID docenteId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!claveValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(respuesta(docentes.obtenerPorId(docenteId)));
    }

    @GetMapping("/perfil/{perfilId}")
    public ResponseEntity<DocenteInternoResponse> obtenerPorPerfilId(
            @PathVariable UUID perfilId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!claveValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(respuesta(docentes.obtenerPorPerfilId(perfilId)));
    }

    private boolean claveValida(String value) {
        return value != null && !value.isBlank() && internalApiKey.equals(value);
    }

    private DocenteInternoResponse respuesta(DocenteResponse docente) {
        return new DocenteInternoResponse(docente.id(), docente.perfilId(), Boolean.TRUE.equals(docente.activo()));
    }
}
