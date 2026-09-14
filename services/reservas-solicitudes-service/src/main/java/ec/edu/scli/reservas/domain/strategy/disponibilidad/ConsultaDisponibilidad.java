package ec.edu.scli.reservas.domain.strategy.disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ConsultaDisponibilidad(
        UUID laboratorioId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin) {
}
