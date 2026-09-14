package ec.edu.scli.academico.presentation.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.HorarioAcademicoService;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;
import jakarta.validation.Valid;
import ec.edu.scli.academico.security.PoliticaAmbitoAcademico;

@RestController
@RequestMapping("/api/v1/horarios")
public class HorarioAcademicoController {

    private final HorarioAcademicoService horarioAcademicoService;
    private final PoliticaAmbitoAcademico politicaAmbito;

    public HorarioAcademicoController(HorarioAcademicoService horarioAcademicoService,
            PoliticaAmbitoAcademico politicaAmbito) {
        this.horarioAcademicoService = horarioAcademicoService;
        this.politicaAmbito = politicaAmbito;
    }

    @PostMapping
    public ResponseEntity<HorarioAcademicoResponse> crear(@Valid @RequestBody HorarioAcademicoRequest request) {
        politicaAmbito.validarMateria(request.materiaId());
        HorarioAcademicoResponse creado = horarioAcademicoService.crear(request);

        URI ubicacion = URI.create("/api/v1/horarios/" + creado.id());

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<HorarioAcademicoResponse>> listar() {
        return ResponseEntity.ok(politicaAmbito.filtrarHorariosLectura(horarioAcademicoService.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioAcademicoResponse> obtenerPorId(@PathVariable UUID id) {
        HorarioAcademicoResponse horario = horarioAcademicoService.obtenerPorId(id);
        politicaAmbito.validarMateriaLectura(horario.materiaId());
        return ResponseEntity.ok(horario);
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<HorarioAcademicoResponse>> listarPorDocente(@PathVariable UUID docenteId) {
        return ResponseEntity.ok(politicaAmbito.filtrarHorariosLectura(
                horarioAcademicoService.listarPorDocente(docenteId)));
    }

    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<List<HorarioAcademicoResponse>> listarPorLaboratorio(@PathVariable UUID laboratorioId) {
        return ResponseEntity.ok(politicaAmbito.filtrarHorariosLectura(
                horarioAcademicoService.listarPorLaboratorio(laboratorioId)));
    }
}
