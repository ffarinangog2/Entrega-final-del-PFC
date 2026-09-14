package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginProtectionService {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final UsuarioAuthRepository users;
    private final IntentoLoginRepository attempts;
    private final Clock clock;

    public LoginProtectionService(UsuarioAuthRepository users, IntentoLoginRepository attempts, Clock clock) {
        this.users = users; this.attempts = attempts; this.clock = clock;
    }

    @Transactional
    public boolean prepareLogin(String identifier) {
        Optional<UsuarioAuth> found = lockByIdentifier(identifier);
        if (found.isEmpty()) return false;
        UsuarioAuth user = found.get();
        OffsetDateTime now = now();
        if (Boolean.TRUE.equals(user.getCuentaBloqueada())) {
            if (user.getBloqueadoHasta() != null && !now.isBefore(user.getBloqueadoHasta())) {
                clearLock(user, now);
                return false;
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean recordFailure(String identifier, LoginMetadata metadata) {
        Optional<UsuarioAuth> found = lockByIdentifier(identifier);
        if (found.isEmpty()) { audit(null, identifier, false, "CREDENCIALES_INVALIDAS", metadata); return false; }
        UsuarioAuth user = found.get();
        OffsetDateTime now = now();
        if (Boolean.TRUE.equals(user.getCuentaBloqueada()) && user.getBloqueadoHasta() != null
                && now.isBefore(user.getBloqueadoHasta())) {
            audit(user.getId(), identifier, false, "CUENTA_BLOQUEADA", metadata);
            return true;
        }
        if (Boolean.TRUE.equals(user.getCuentaBloqueada())) clearLock(user, now);
        int failures = Math.min(MAX_FAILED_ATTEMPTS, Optional.ofNullable(user.getIntentosFallidos()).orElse(0) + 1);
        user.setIntentosFallidos(failures);
        boolean blocked = failures >= MAX_FAILED_ATTEMPTS;
        if (blocked) { user.setCuentaBloqueada(true); user.setBloqueadoHasta(now.plus(LOCK_DURATION)); }
        user.setActualizadoEn(now);
        audit(user.getId(), identifier, false, blocked ? "BLOQUEO_TEMPORAL" : "CREDENCIALES_INVALIDAS", metadata);
        return blocked;
    }

    @Transactional
    public void recordSuccess(UUID userId, String identifier, LoginMetadata metadata) {
        users.findLockedById(userId).ifPresent(user -> {
            OffsetDateTime now = now(); clearLock(user, now); user.setUltimoLogin(now);
            audit(userId, identifier, true, "LOGIN_EXITOSO", metadata);
        });
    }

    @Transactional
    public boolean ensureNotLocked(UUID userId) {
        Optional<UsuarioAuth> found = users.findLockedById(userId);
        if (found.isEmpty()) return false;
        UsuarioAuth user = found.get(); OffsetDateTime now = now();
        if (!Boolean.TRUE.equals(user.getCuentaBloqueada())) return false;
        if (user.getBloqueadoHasta() != null && !now.isBefore(user.getBloqueadoHasta())) {
            clearLock(user, now); return false;
        }
        return true;
    }

    private Optional<UsuarioAuth> lockByIdentifier(String value) {
        String normalized = value.trim();
        return users.findLockedByUsernameIgnoreCase(normalized).or(() -> users.findLockedByEmailIgnoreCase(normalized));
    }
    private void clearLock(UsuarioAuth user, OffsetDateTime now) {
        user.setIntentosFallidos(0); user.setCuentaBloqueada(false); user.setBloqueadoHasta(null); user.setActualizadoEn(now);
    }
    private void audit(UUID userId, String identifier, boolean success, String reason, LoginMetadata metadata) {
        IntentoLogin row = new IntentoLogin(); row.setId(UUID.randomUUID()); row.setUsuarioId(userId);
        row.setUsernameIngresado(limit(identifier, 160)); row.setExitoso(success); row.setMotivo(reason);
        row.setIpAddress(limit(metadata.ipAddress(), 64)); row.setUserAgent(limit(metadata.userAgent(), 500)); row.setFechaHora(now()); attempts.save(row);
    }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private static String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }

    public record LoginMetadata(String ipAddress, String userAgent) {
        public static LoginMetadata unknown() { return new LoginMetadata(null, null); }
    }
}
