package ec.edu.uteq.scli.auth_service.domain.repository;

import ec.edu.uteq.scli.auth_service.domain.model.RefreshSession;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {
    RefreshSession guardar(RefreshSession session);
    Optional<RefreshSession> buscarPorTokenHash(String tokenHash);
    boolean revocarSiActiva(String tokenHash, OffsetDateTime ahora);
    void registrarReemplazo(String tokenHash, UUID reemplazoId);
    long contarActivas(OffsetDateTime ahora);
}
