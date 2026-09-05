package ec.edu.scli.academico.presentation.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.application.service.CarreraService;
import ec.edu.scli.academico.dto.internal.CarreraEstadoResponse;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.dto.internal.MateriaContextoResponse;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;

/**
 * Endpoints internos consumidos por Reservas y Solicitudes Service.
 * Solo entregan información estructural y de estado general;
 * la disponibilidad por fecha/hora específica NO es responsabilidad
 * de este microservicio.
 */
@RestController
@RequestMapping("/api/v1/internal")
public class InternalController {

    private final LaboratorioService laboratorioService;
    private final MateriaService materiaService;
    private final PeriodoLectivoService periodoLectivoService;
    private final CarreraService carreraService;
    private final String internalApiKey;

    public InternalController(
            LaboratorioService laboratorioService,
            MateriaService materiaService,
            PeriodoLectivoService periodoLectivoService, CarreraService carreraService,
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        this.laboratorioService = laboratorioService;
        this.materiaService = materiaService;
        this.periodoLectivoService = periodoLectivoService;
        this.carreraService = carreraService;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/laboratorios/{id}/disponibilidad-base")
    public ResponseEntity<LaboratorioDisponibilidadBaseResponse> disponibilidadBaseLaboratorio(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey
    ) {
        if (!esApiKeyValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(laboratorioService.obtenerDisponibilidadBase(id));
    }

    @GetMapping("/laboratorios/{id}/exists")
    public ResponseEntity<ExisteResponse> existeLaboratorio(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey
    ) {
        if (!esApiKeyValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(laboratorioService.verificarExistencia(id));
    }

    @GetMapping("/materias/{id}/exists")
    public ResponseEntity<ExisteResponse> existeMateria(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey
    ) {
        if (!esApiKeyValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(materiaService.verificarExistencia(id));
    }

    @GetMapping("/materias/{id}/contexto")
    public ResponseEntity<MateriaContextoResponse> contextoMateria(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!esApiKeyValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var materia = materiaService.obtenerPorId(id);
            return ResponseEntity.ok(new MateriaContextoResponse(
                    materia.id(), materia.carreraId(), materia.nivel(), true, Boolean.TRUE.equals(materia.activo())));
        } catch (ec.edu.scli.academico.domain.exception.ResourceNotFoundException exception) {
            return ResponseEntity.ok(new MateriaContextoResponse(id, null, null, false, false));
        }
    }

    @GetMapping("/periodos-lectivos/{id}/exists")
    public ResponseEntity<ExisteResponse> existePeriodoLectivo(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey
    ) {
        if (!esApiKeyValida(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(periodoLectivoService.verificarExistencia(id));
    }
    @GetMapping("/periodos-lectivos/{id}")
    public ResponseEntity<PeriodoLectivoResponse> periodoLectivo(@PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!esApiKeyValida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(periodoLectivoService.obtenerPorId(id));
    }
    @GetMapping("/periodos-lectivos/actual/contexto")
    public ResponseEntity<PeriodoLectivoResponse> periodoActual(@RequestHeader(value="X-Internal-Api-Key",required=false) String apiKey){
        if(!esApiKeyValida(apiKey))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(periodoLectivoService.obtenerActual());
    }

    @GetMapping("/carreras/{id}/estado")
    public ResponseEntity<CarreraEstadoResponse> estadoCarrera(@PathVariable UUID id,
            @RequestHeader(value="X-Internal-Api-Key",required=false) String apiKey) {
        if (!esApiKeyValida(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            var carrera = carreraService.obtenerPorId(id);
            return ResponseEntity.ok(new CarreraEstadoResponse(id, true, Boolean.TRUE.equals(carrera.activo())));
        } catch (ec.edu.scli.academico.domain.exception.ResourceNotFoundException exception) {
            return ResponseEntity.ok(new CarreraEstadoResponse(id, false, false));
        }
    }

    private boolean esApiKeyValida(String apiKey) {
        return apiKey != null && internalApiKey.equals(apiKey);
    }
}
