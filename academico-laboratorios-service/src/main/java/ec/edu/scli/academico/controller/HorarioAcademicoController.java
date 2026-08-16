package ec.edu.scli.academico.controller;

import ec.edu.scli.academico.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.dto.horario.HorarioAcademicoResponse;
import ec.edu.scli.academico.service.HorarioAcademicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/horarios")
public class HorarioAcademicoController {

    private final HorarioAcademicoService horarioAcademicoService;

    public HorarioAcademicoController(HorarioAcademicoService horarioAcademicoService) {
        this.horarioAcademicoService = horarioAcademicoService;
    }

    @PostMapping
    public ResponseEntity<HorarioAcademicoResponse> crear(@Valid @RequestBody HorarioAcademicoRequest request) {

        HorarioAcademicoResponse creado = horarioAcademicoService.crear(request);

        URI ubicacion = URI.create("/api/v1/horarios/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<HorarioAcademicoResponse>> listar() {
        return ResponseEntity.ok(horarioAcademicoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioAcademicoResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(horarioAcademicoService.obtenerPorId(id));
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<HorarioAcademicoResponse>> listarPorDocente(@PathVariable UUID docenteId) {
        return ResponseEntity.ok(horarioAcademicoService.listarPorDocente(docenteId));
    }

    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<List<HorarioAcademicoResponse>> listarPorLaboratorio(@PathVariable UUID laboratorioId) {
        return ResponseEntity.ok(horarioAcademicoService.listarPorLaboratorio(laboratorioId));
    }
}
