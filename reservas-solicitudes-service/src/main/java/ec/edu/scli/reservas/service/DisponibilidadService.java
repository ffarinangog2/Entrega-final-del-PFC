package ec.edu.scli.reservas.service;

import ec.edu.scli.reservas.dto.response.DisponibilidadResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Define la consulta de disponibilidad de un laboratorio. */
public interface DisponibilidadService {

    DisponibilidadResponse consultar(UUID laboratorioId, LocalDate fecha, LocalTime horaInicio,
                                     LocalTime horaFin);
}
