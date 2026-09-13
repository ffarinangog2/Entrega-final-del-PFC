package ec.edu.scli.reservas.domain.port.out;

import java.time.LocalDate;
import java.util.UUID;

public interface AgendaMutexPort {
    void bloquear(UUID laboratorioId, LocalDate fecha);
}
