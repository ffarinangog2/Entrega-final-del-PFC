package ec.edu.scli.reservas.domain.port.out;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface BloqueoAgendaRepositoryPort {
    List<BloqueoAgenda> buscarActivos(UUID laboratorioId, LocalDate desde, LocalDate hasta);
    long contarActivosConflictivos(UUID laboratorioId, LocalDate fecha, LocalTime inicio, LocalTime fin);
    Optional<BloqueoAgenda> buscarPorId(UUID id);
    BloqueoAgenda guardar(BloqueoAgenda bloqueo);
}
