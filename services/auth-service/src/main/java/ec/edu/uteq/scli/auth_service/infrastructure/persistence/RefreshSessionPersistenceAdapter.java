package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import ec.edu.uteq.scli.auth_service.domain.model.RefreshSession;
import ec.edu.uteq.scli.auth_service.domain.repository.RefreshSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshSessionPersistenceAdapter implements RefreshSessionRepository {
    private final RefreshTokenJpaRepository repository;

    public RefreshSessionPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshSession guardar(RefreshSession session) {
        return toDomain(repository.saveAndFlush(toEntity(session)));
    }

    @Override
    public Optional<RefreshSession> buscarPorTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public boolean revocarSiActiva(String tokenHash, OffsetDateTime ahora) {
        return repository.revocarSiActiva(tokenHash, ahora) == 1;
    }

    @Override
    public void registrarReemplazo(String tokenHash, UUID reemplazoId) {
        repository.registrarReemplazo(tokenHash, reemplazoId);
    }

    @Override
    public long contarActivas(OffsetDateTime ahora) {
        return repository.contarActivas(ahora);
    }

    private RefreshToken toEntity(RefreshSession session) {
        RefreshToken entity = new RefreshToken();
        entity.setId(session.id());
        entity.setUsuarioId(session.usuarioId());
        entity.setTokenHash(session.tokenHash());
        entity.setFamiliaToken(session.familiaToken());
        entity.setEmitidoEn(session.emitidoEn());
        entity.setExpiraEn(session.expiraEn());
        entity.setRevocado(session.revocado());
        entity.setRevocadoEn(session.revocadoEn());
        entity.setReemplazadoPor(session.reemplazadoPor());
        return entity;
    }

    private RefreshSession toDomain(RefreshToken entity) {
        return new RefreshSession(entity.getId(), entity.getUsuarioId(), entity.getTokenHash(),
                entity.getFamiliaToken(), entity.getEmitidoEn(), entity.getExpiraEn(),
                Boolean.TRUE.equals(entity.getRevocado()), entity.getRevocadoEn(), entity.getReemplazadoPor());
    }
}
