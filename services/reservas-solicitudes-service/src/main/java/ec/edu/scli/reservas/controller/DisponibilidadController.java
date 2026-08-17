package ec.edu.scli.reservas.controller;

import ec.edu.scli.reservas.dto.response.DisponibilidadResponse;
import ec.edu.scli.reservas.service.DisponibilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Expone la consulta REST de disponibilidad de laboratorios. */
@RestController
@Validated
@RequestMapping("/api/v1/disponibilidad")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/laboratorios/{laboratorioId}")
    public ResponseEntity<DisponibilidadResponse> consultar(
            @PathVariable UUID laboratorioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {
        if (!horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("La hora final debe ser mayor que la hora inicial");
        }
        return ResponseEntity.ok(disponibilidadService.consultar(laboratorioId, fecha, horaInicio, horaFin));
    }
}
