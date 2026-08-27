package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.domain.model.RefreshSession;
import ec.edu.uteq.scli.auth_service.domain.repository.RefreshSessionRepository;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshSessionService {
    private final RefreshSessionRepository repository;
    private final Clock clock;

    public RefreshSessionService(RefreshSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(UUID usuarioId, String token, Instant emitidoEn, Instant expiraEn) {
        OffsetDateTime emitido = OffsetDateTime.ofInstant(emitidoEn, ZoneOffset.UTC);
        OffsetDateTime expiracion = OffsetDateTime.ofInstant(expiraEn, ZoneOffset.UTC);
        repository.guardar(new RefreshSession(UUID.randomUUID(), usuarioId, hash(token), UUID.randomUUID(),
                emitido, expiracion, false, null, null));
    }

    @Transactional(readOnly = true)
    public RefreshSession validarActiva(UUID usuarioId, String token) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        RefreshSession session = repository.buscarPorTokenHash(hash(token))
                .orElseThrow(InvalidCredentialsException::new);
        if (!session.usuarioId().equals(usuarioId) || session.revocado() || !session.expiraEn().isAfter(ahora)) {
            throw new InvalidCredentialsException();
        }
        return session;
    }

    @Transactional
    public void rotar(RefreshSession anterior, String tokenAnterior, String tokenNuevo,
                      Instant emitidoEn, Instant expiraEn) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        String hashAnterior = hash(tokenAnterior);
        if (!repository.revocarSiActiva(hashAnterior, ahora)) {
            throw new InvalidCredentialsException();
        }
        RefreshSession nueva = repository.guardar(new RefreshSession(UUID.randomUUID(), anterior.usuarioId(),
                hash(tokenNuevo), anterior.familiaToken(), OffsetDateTime.ofInstant(emitidoEn, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(expiraEn, ZoneOffset.UTC), false, null, null));
        repository.registrarReemplazo(hashAnterior, nueva.id());
    }

    @Transactional
    public void revocarIdempotente(String token) {
        repository.revocarSiActiva(hash(token), OffsetDateTime.now(clock));
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
