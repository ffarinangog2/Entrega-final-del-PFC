package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import ec.edu.uteq.scli.auth_service.domain.model.RefreshSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshSessionPersistenceAdapterTest {
    @Mock private RefreshTokenJpaRepository jpaRepository;

    @Test
    void adaptaGuardadoBusquedaRevocacionReemplazoYConteo() {
        RefreshSessionPersistenceAdapter adapter = new RefreshSessionPersistenceAdapter(jpaRepository);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RefreshSession session = new RefreshSession(UUID.randomUUID(), UUID.randomUUID(), "hash",
                UUID.randomUUID(), now, now.plusHours(1), false, null, null);
        when(jpaRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jpaRepository.findByTokenHash("hash")).thenAnswer(invocation -> {
            RefreshToken entity = new RefreshToken();
            entity.setId(session.id());
            entity.setUsuarioId(session.usuarioId());
            entity.setTokenHash(session.tokenHash());
            entity.setFamiliaToken(session.familiaToken());
            entity.setEmitidoEn(session.emitidoEn());
            entity.setExpiraEn(session.expiraEn());
            entity.setRevocado(false);
            return Optional.of(entity);
        });
        when(jpaRepository.revocarSiActiva("hash", now)).thenReturn(1);
        when(jpaRepository.contarActivas(now)).thenReturn(3L);

        assertEquals(session, adapter.guardar(session));
        assertEquals(session, adapter.buscarPorTokenHash("hash").orElseThrow());
        assertTrue(adapter.revocarSiActiva("hash", now));
        assertEquals(3L, adapter.contarActivas(now));
        adapter.registrarReemplazo("hash", session.id());

        verify(jpaRepository).registrarReemplazo("hash", session.id());
    }
}
