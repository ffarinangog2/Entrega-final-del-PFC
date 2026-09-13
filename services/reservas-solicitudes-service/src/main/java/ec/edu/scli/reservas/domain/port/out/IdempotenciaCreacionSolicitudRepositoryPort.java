package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.IdempotenciaCreacionSolicitud;

import java.util.Optional;
import java.util.UUID;

public interface IdempotenciaCreacionSolicitudRepositoryPort {
    void registrarSiAusente(String clave, UUID actorId, String payloadHash);
    Optional<IdempotenciaCreacionSolicitud> buscarParaActualizar(String clave);
    void completar(String clave, UUID solicitudId);
}
