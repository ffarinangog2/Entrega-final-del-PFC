package ec.edu.uteq.scli.auth_service.application;

import ec.edu.uteq.scli.auth_service.application.service.PasswordResetTokenManager;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.mockito.Mockito.*;

class PasswordResetTokenManagerTest {
    @Test void reemplazoInvalidaAnterioresAntesDeGuardarNuevo() {
        PasswordResetTokenRepository repository=mock(PasswordResetTokenRepository.class);
        PasswordResetTokenManager manager=new PasswordResetTokenManager(repository);
        PasswordResetToken token=new PasswordResetToken(); token.setUsuarioId(UUID.randomUUID()); token.setCreadoEn(OffsetDateTime.now());
        manager.replaceActive(token);
        var order=inOrder(repository); order.verify(repository).invalidateActive(token.getUsuarioId(),token.getCreadoEn()); order.verify(repository).saveAndFlush(token);
    }
}
