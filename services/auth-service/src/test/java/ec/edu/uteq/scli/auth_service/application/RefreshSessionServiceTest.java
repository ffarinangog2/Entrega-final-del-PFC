package ec.edu.uteq.scli.auth_service.application;

import ec.edu.uteq.scli.auth_service.application.service.RefreshSessionService;
import ec.edu.uteq.scli.auth_service.domain.model.RefreshSession;
import ec.edu.uteq.scli.auth_service.domain.repository.RefreshSessionRepository;
import ec.edu.uteq.scli.auth_service.domain.service.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceTest {
    @Mock private RefreshSessionRepository repository;
    private RefreshSessionService service;
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new RefreshSessionService(repository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void loginRegistraHashSinPersistirTokenPlano() {
        UUID usuarioId = UUID.randomUUID();
        when(repository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.registrar(usuarioId, "refresh-secreto", now, now.plusSeconds(60));

        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository).guardar(captor.capture());
        assertEquals(usuarioId, captor.getValue().usuarioId());
        assertNotEquals("refresh-secreto", captor.getValue().tokenHash());
        assertEquals(64, captor.getValue().tokenHash().length());
    }

    @Test
    void sesionRevocadaYDesconocidaFallan() {
        UUID usuarioId = UUID.randomUUID();
        RefreshSession revocada = session(usuarioId, true, now.plusSeconds(60));
        when(repository.buscarPorTokenHash(any())).thenReturn(Optional.of(revocada), Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> service.validarActiva(usuarioId, "token-1"));
        assertThrows(InvalidCredentialsException.class, () -> service.validarActiva(usuarioId, "token-2"));
    }

    @Test
    void sesionExpiradaFalla() {
        UUID usuarioId = UUID.randomUUID();
        when(repository.buscarPorTokenHash(any())).thenReturn(Optional.of(session(usuarioId, false, now)));

        assertThrows(InvalidCredentialsException.class, () -> service.validarActiva(usuarioId, "token"));
    }

    @Test
    void rotacionRevocaAnteriorYPreservaFamilia() {
        UUID usuarioId = UUID.randomUUID();
        RefreshSession anterior = session(usuarioId, false, now.plusSeconds(60));
        when(repository.revocarSiActiva(any(), any())).thenReturn(true);
        when(repository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.rotar(anterior, "anterior", "nuevo", now, now.plusSeconds(120));

        ArgumentCaptor<RefreshSession> captor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository).guardar(captor.capture());
        assertEquals(anterior.familiaToken(), captor.getValue().familiaToken());
        verify(repository).registrarReemplazo(any(), eq(captor.getValue().id()));
    }

    @Test
    void rotacionConcurrenteORevocadaFalla() {
        RefreshSession anterior = session(UUID.randomUUID(), false, now.plusSeconds(60));
        when(repository.revocarSiActiva(any(), any())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> service.rotar(anterior, "anterior", "nuevo", now, now.plusSeconds(120)));
        verify(repository, never()).guardar(any());
    }

    @Test
    void logoutEsIdempotente() {
        service.revocarIdempotente("token");
        service.revocarIdempotente("token");
        verify(repository, times(2)).revocarSiActiva(any(), any());
    }

    private RefreshSession session(UUID usuarioId, boolean revocada, Instant expira) {
        return new RefreshSession(UUID.randomUUID(), usuarioId, "hash", UUID.randomUUID(),
                OffsetDateTime.ofInstant(now.minusSeconds(10), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(expira, ZoneOffset.UTC), revocada,
                revocada ? OffsetDateTime.ofInstant(now, ZoneOffset.UTC) : null, null);
    }
}
