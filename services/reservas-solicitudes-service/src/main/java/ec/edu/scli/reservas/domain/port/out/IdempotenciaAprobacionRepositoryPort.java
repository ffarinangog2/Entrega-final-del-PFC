package ec.edu.scli.reservas.domain.port.out;

import ec.edu.scli.reservas.domain.model.IdempotenciaAprobacion;

import java.util.Optional;
import java.util.UUID;

/** Persistencia de claves idempotentes para la operación de aprobación. */
public interface IdempotenciaAprobacionRepositoryPort {

    void registrarSiAusente(String clave, UUID solicitudId);

    Optional<IdempotenciaAprobacion> buscarParaActualizar(String clave);

    void completar(String clave, UUID reservaId);
}
