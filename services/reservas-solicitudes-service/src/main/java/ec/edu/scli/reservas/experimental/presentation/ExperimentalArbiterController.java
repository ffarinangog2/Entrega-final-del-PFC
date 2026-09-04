package ec.edu.scli.reservas.experimental.presentation;

import ec.edu.scli.reservas.experimental.application.*;
import ec.edu.scli.reservas.experimental.domain.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/experimentos/arbiter")
@ConditionalOnProperty(name = "app.experimental.arbiter.enabled", havingValue = "true")
public class ExperimentalArbiterController {
    private final ExperimentalArbiterService service;
    private final String internalKey;
    public ExperimentalArbiterController(ExperimentalArbiterService service,
            @Value("${app.internal-api-key}") String internalKey) {
        this.service = service; this.internalKey = internalKey;
    }
    @PostMapping("/adjudicar")
    public ResponseEntity<ResultadoArbitraje> adjudicar(@RequestHeader(value="X-Internal-Api-Key", required=false) String key,
            @Valid @RequestBody AdjudicacionExperimentalRequest request) {
        authorize(key);
        var domain = new SolicitudArbitraje(request.runId(), request.requestId(), request.equipmentId(),
                request.laboratorioId(), request.agenteId(), request.inicio(), request.fin());
        return ResponseEntity.ok(service.adjudicar(domain, request.equipmentStatus(), request.equipmentActive()));
    }
    @PostMapping("/lider/fallo")
    public ResponseEntity<Map<String, Object>> fallarLider(
            @RequestHeader(value="X-Internal-Api-Key", required=false) String key) {
        authorize(key);
        if (!(service.selected() instanceof S3BullyLamportStrategy s3))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "La caída del líder de aplicación solo corresponde a S3"));
        int previous = s3.cluster().leaderId(); long recovery = s3.fallarLider();
        return ResponseEntity.ok(Map.of("previousLeaderId", previous, "leaderId", s3.cluster().leaderId(),
                "recoveryMs", recovery, "recoverySeconds", recovery / 1000.0));
    }
    private void authorize(String supplied) {
        if (supplied == null || internalKey == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), internalKey.getBytes(StandardCharsets.UTF_8)))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "API key interna invalida");
    }
}
