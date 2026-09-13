package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.PasswordResetToken;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PasswordResetTokenManager {
    private final PasswordResetTokenRepository tokens;
    public PasswordResetTokenManager(PasswordResetTokenRepository tokens) { this.tokens = tokens; }

    @Transactional
    public void replaceActive(PasswordResetToken token) {
        tokens.invalidateActive(token.getUsuarioId(), token.getCreadoEn());
        tokens.saveAndFlush(token);
    }

    @Transactional
    public void invalidate(UUID tokenId, OffsetDateTime now) {
        tokens.invalidateById(tokenId, now);
    }
}
