package ec.edu.scli.reservas.domain.strategy.disponibilidad;

import java.time.LocalDate;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public interface DisponibilidadStrategy {
    ResultadoDisponibilidad evaluar(
            ConsultaDisponibilidad consulta,
            LocalDate fechaActual,
            Supplier<EstadoLaboratorio> laboratorio,
            LongSupplier conflictosReserva,
            LongSupplier bloqueosAgenda);
}
