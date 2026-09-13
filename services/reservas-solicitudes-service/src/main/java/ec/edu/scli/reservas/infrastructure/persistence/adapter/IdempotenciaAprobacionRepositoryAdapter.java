package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.IdempotenciaAprobacion;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaAprobacionRepositoryPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.IdempotenciaAprobacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.IdempotenciaAprobacionSpringDataRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotenciaAprobacionRepositoryAdapter
        implements IdempotenciaAprobacionRepositoryPort {

    private final IdempotenciaAprobacionSpringDataRepository repository;

    public IdempotenciaAprobacionRepositoryAdapter(
            IdempotenciaAprobacionSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrarSiAusente(String clave, UUID solicitudId) {
        repository.insertarSiAusente(clave, solicitudId);
    }

    @Override
    public Optional<IdempotenciaAprobacion> buscarParaActualizar(String clave) {
        return repository.findByClaveForUpdate(clave).map(this::toDomain);
    }

    @Override
    public void completar(String clave, UUID reservaId) {
        if (repository.completar(clave, reservaId) != 1) {
            throw new IllegalStateException(
                    "La operación idempotente de aprobación ya fue completada");
        }
    }

    private IdempotenciaAprobacion toDomain(IdempotenciaAprobacionJpaEntity entity) {
        return new IdempotenciaAprobacion(
                entity.getClave(),
                entity.getOperacion(),
                entity.getSolicitudId(),
                entity.getReservaId());
    }
}
