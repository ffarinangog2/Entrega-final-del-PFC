package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoEstadoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import jakarta.validation.Valid;
import ec.edu.scli.academico.security.PoliticaAmbitoAcademico;

@RestController
public class EquipoController {

    private final EquipoService equipoService;
    private final PoliticaAmbitoAcademico politicaAmbito;

    public EquipoController(EquipoService equipoService, PoliticaAmbitoAcademico politicaAmbito) {
        this.equipoService = equipoService;
        this.politicaAmbito = politicaAmbito;
    }

    @PostMapping("/api/v1/equipos")
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest request) {
        politicaAmbito.validarLaboratorio(request.laboratorioId());
        EquipoResponse creado = equipoService.crear(request);

        URI ubicacion = URI.create("/api/v1/equipos/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/api/v1/equipos")
    public ResponseEntity<Page<EquipoResponse>> listar(
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) EstadoEquipo estado,
            @RequestParam(required = false) Boolean activo,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(equipoService.listar(laboratorioId, estado, activo, pageable));
    }

    @GetMapping("/api/v1/equipos/{id}")
    public ResponseEntity<EquipoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(equipoService.obtenerPorId(id));
    }

    @GetMapping("/api/v1/laboratorios/{laboratorioId}/equipos")
    public ResponseEntity<List<EquipoResponse>> listarPorLaboratorio(@PathVariable UUID laboratorioId) {
        return ResponseEntity.ok(equipoService.listarPorLaboratorio(laboratorioId));
    }

    @PutMapping("/api/v1/equipos/{id}")
    public ResponseEntity<EquipoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EquipoRequest request
    ) {
        politicaAmbito.validarLaboratorio(equipoService.obtenerPorId(id).laboratorioId());
        politicaAmbito.validarLaboratorio(request.laboratorioId());
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    @PatchMapping("/api/v1/equipos/{id}/estado")
    public ResponseEntity<EquipoResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EquipoEstadoRequest request
    ) {
        politicaAmbito.validarLaboratorio(equipoService.obtenerPorId(id).laboratorioId());
        return ResponseEntity.ok(equipoService.cambiarEstado(id, request.estado()));
    }
}
