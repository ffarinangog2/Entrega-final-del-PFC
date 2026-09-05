package ec.edu.scli.reservas.infrastructure.persistence.adapter;
import ec.edu.scli.reservas.domain.port.out.BloqueoAgendaRepositoryPort;
import ec.edu.scli.reservas.entity.BloqueoAgenda;
import ec.edu.scli.reservas.repository.BloqueoAgendaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Component
public class BloqueoAgendaRepositoryAdapter implements BloqueoAgendaRepositoryPort {
    private final BloqueoAgendaRepository repository;
    public BloqueoAgendaRepositoryAdapter(BloqueoAgendaRepository repository) { this.repository = repository; }
    public List<BloqueoAgenda> buscarActivos(UUID laboratorioId, LocalDate desde, LocalDate hasta) {
        Specification<BloqueoAgenda> spec = Specification.allOf(
                (root, query, cb) -> cb.isTrue(root.get("activo")),
                laboratorioId == null ? null : (root, query, cb) -> cb.equal(root.get("laboratorioId"), laboratorioId),
                desde == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde),
                hasta == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        return repository.findAll(spec);
    }
    public long contarActivosConflictivos(UUID l, LocalDate f, LocalTime i, LocalTime fin) { return repository.contarBloqueosActivosConflictivos(l, f, i, fin); }
    public Optional<BloqueoAgenda> buscarPorId(UUID id) { return repository.findById(id); }
    public BloqueoAgenda guardar(BloqueoAgenda bloqueo) { return repository.save(bloqueo); }
}
