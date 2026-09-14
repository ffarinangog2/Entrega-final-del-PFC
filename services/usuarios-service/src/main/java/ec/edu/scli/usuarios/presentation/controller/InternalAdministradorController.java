package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/administradores")
public class InternalAdministradorController {
    private final AdministradorRepository administradores;
    private final String internalApiKey;

    public InternalAdministradorController(AdministradorRepository administradores,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.administradores = administradores;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/por-piso")
    public ResponseEntity<List<UUID>> perfilesPorPiso(@RequestParam UUID pisoId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String key) {
        if (key == null || !internalApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(administradores.findByPisoIdAndActivoTrue(pisoId).stream()
                .map(item -> item.getPerfil().getId()).toList());
    }
}
