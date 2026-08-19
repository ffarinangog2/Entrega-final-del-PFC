package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoResponse;
import ec.edu.scli.academico.service.TipoEquipoService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tipos-equipo")
public class TipoEquipoController {

    private final TipoEquipoService tipoEquipoService;

    public TipoEquipoController(TipoEquipoService tipoEquipoService) {
        this.tipoEquipoService = tipoEquipoService;
    }

    @PostMapping
    public ResponseEntity<TipoEquipoResponse> crear(@Valid @RequestBody TipoEquipoRequest request) {

        TipoEquipoResponse creado = tipoEquipoService.crear(request);

        URI ubicacion = URI.create("/api/v1/tipos-equipo/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<Page<TipoEquipoResponse>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(tipoEquipoService.listar(codigo, nombre, activo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoEquipoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(tipoEquipoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoEquipoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TipoEquipoRequest request
    ) {
        return ResponseEntity.ok(tipoEquipoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        tipoEquipoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
