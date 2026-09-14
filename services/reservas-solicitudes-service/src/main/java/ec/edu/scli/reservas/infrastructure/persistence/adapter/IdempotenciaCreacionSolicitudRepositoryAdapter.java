package ec.edu.scli.reservas.infrastructure.persistence.adapter;

import ec.edu.scli.reservas.domain.model.IdempotenciaCreacionSolicitud;
import ec.edu.scli.reservas.domain.port.out.IdempotenciaCreacionSolicitudRepositoryPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.IdempotenciaCreacionSolicitudJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.IdempotenciaCreacionSolicitudSpringDataRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotenciaCreacionSolicitudRepositoryAdapter
        implements IdempotenciaCreacionSolicitudRepositoryPort {
    private final IdempotenciaCreacionSolicitudSpringDataRepository repository;

    public IdempotenciaCreacionSolicitudRepositoryAdapter(
            IdempotenciaCreacionSolicitudSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrarSiAusente(String clave, UUID actorId, String payloadHash) {
        repository.insertarSiAusente(clave, actorId, payloadHash);
    }

    @Override
    public Optional<IdempotenciaCreacionSolicitud> buscarParaActualizar(String clave) {
        return repository.buscarParaActualizar(clave).map(this::toDomain);
    }

    @Override
    public void completar(String clave, UUID solicitudId) {
        if (repository.completar(clave, solicitudId) != 1) {
            throw new IllegalStateException("La creación idempotente ya fue completada");
        }
    }

    private IdempotenciaCreacionSolicitud toDomain(IdempotenciaCreacionSolicitudJpaEntity entity) {
        return new IdempotenciaCreacionSolicitud(entity.getClave(), entity.getOperacion(),
                entity.getActorId(), entity.getPayloadHash(), entity.getSolicitudId());
    }
}
