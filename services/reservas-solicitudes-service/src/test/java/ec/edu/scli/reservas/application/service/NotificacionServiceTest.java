package ec.edu.scli.reservas.application.service;

import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import ec.edu.scli.reservas.infrastructure.persistence.entity.DispositivoNotificacionJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.entity.NotificacionInternaJpaEntity;
import ec.edu.scli.reservas.infrastructure.persistence.repository.DispositivoNotificacionRepository;
import ec.edu.scli.reservas.infrastructure.persistence.repository.NotificacionInternaJpaRepository;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {
    @Mock
    DispositivoNotificacionRepository repository;
    @Mock
    NotificationPort sender;
    @Mock
    NotificacionInternaJpaRepository bandeja;
    NotificacionService service;

    @BeforeEach
    void init() {
        service = new NotificacionService(repository, sender, bandeja);
    }

    @Test
    void registrarAsociaIdentidadAutenticadaYReactivaToken() {
        UUID user = UUID.randomUUID(), profile = UUID.randomUUID();
        var e = new DispositivoNotificacionJpaEntity();
        when(repository.findByToken("token")).thenReturn(Optional.of(e));
        when(repository.saveAndFlush(e)).thenReturn(e);
        service.registrar(new RegistrarDispositivoRequest("token", "ANDROID"), user, profile);
        assertEquals(user, e.getUsuarioAuthId());
        assertEquals(profile, e.getPerfilId());
        assertTrue(e.isActivo());
    }

    @Test
    void senderNoPropagaFalloNiExponeToken() {
        UUID profile = UUID.randomUUID();
        var e = new DispositivoNotificacionJpaEntity();
        e.setToken("secreto");
        when(repository.findByPerfilIdAndActivoTrue(profile)).thenReturn(List.of(e));
        doThrow(new IllegalStateException()).when(sender).enviar(any(), any(), any(), any());
        assertDoesNotThrow(() -> service.notificarPerfil(profile, "t", "c", Map.of()));
    }

    @Test
    void desregistrarSoloDesactivaTokenDelPerfilAutenticado() {
        UUID profile = UUID.randomUUID();
        var e = new DispositivoNotificacionJpaEntity();
        e.setActivo(true);
        when(repository.findByTokenAndPerfilId("token", profile)).thenReturn(Optional.of(e));
        service.desregistrar("token", profile);
        assertFalse(e.isActivo());
        verify(repository).save(e);
    }

    @Test
    void desregistrarNoHaceNadaSiYaEstabaInactivo() {
        UUID profile = UUID.randomUUID();
        var e = new DispositivoNotificacionJpaEntity();
        e.setActivo(false);
        when(repository.findByTokenAndPerfilId("token", profile)).thenReturn(Optional.of(e));
        service.desregistrar("token", profile);
        assertFalse(e.isActivo());
        verify(repository, never()).save(e);
    }

    @Test
    void desregistrarEsIdempotenteYNoAfectaTokenAjeno() {
        UUID profile = UUID.randomUUID();
        when(repository.findByTokenAndPerfilId("token", profile)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.desregistrar("token", profile));
        verify(repository, never()).save(any());
    }

    @Test
    void claveDeEventoEvitaDuplicados() {
        UUID perfil = UUID.randomUUID();
        when(bandeja.existsByClaveEvento("evento")).thenReturn(true);
        service.notificarPerfilIdempotente(perfil, "evento", "t", "c", Map.of());
        verify(bandeja, never()).save(any());
        verify(sender, never()).enviar(any(), any(), any(), any());
    }

    @Test
    void lecturaEstaAisladaPorPerfil() {
        UUID id = UUID.randomUUID(), perfil = UUID.randomUUID();
        when(bandeja.findByIdAndPerfilId(id, perfil)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.leer(id, perfil));
    }

    @Test
    void notificarPerfilIdempotenteGuardaConReferenciaYToleraUUIDInvalido() {
        UUID perfil = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        when(bandeja.existsByClaveEvento("clave-valida")).thenReturn(false);

        // Caso 1: referenciaId con UUID valido
        service.notificarPerfilIdempotente(perfil, "clave-valida", "titulo", "cuerpo",
                Map.of("tipo", "AVISO", "referenciaId", refId.toString()));
        verify(bandeja).save(any(NotificacionInternaJpaEntity.class));

        // Caso 2: referenciaId con formato no UUID (debe capturar IllegalArgumentException silenciosamente)
        when(bandeja.existsByClaveEvento("clave-invalida")).thenReturn(false);
        service.notificarPerfilIdempotente(perfil, "clave-invalida", "titulo", "cuerpo",
                Map.of("tipo", "AVISO", "referenciaId", "no-es-uuid"));

        // Caso 3: clave nula (se guarda sin clave de deduplicacion)
        service.notificarPerfilIdempotente(perfil, null, "titulo", "cuerpo",
                Map.of("tipo", "AVISO", "planificacionId", refId.toString()));
    }

    @Test
    void listarYContarNoLeidas() {
        UUID perfil = UUID.randomUUID();
        var n = new NotificacionInternaJpaEntity();
        n.setTitulo("Aviso");
        when(bandeja.findTop50ByPerfilIdOrderByCreadaEnDesc(perfil)).thenReturn(List.of(n));
        when(bandeja.countByPerfilIdAndLeidaFalse(perfil)).thenReturn(3L);

        assertEquals(1, service.listar(perfil).size());
        assertEquals(3L, service.noLeidas(perfil));
    }

    @Test
    void leerYLeerTodas() {
        UUID id = UUID.randomUUID(), perfil = UUID.randomUUID();
        var n = new NotificacionInternaJpaEntity();
        n.setLeida(false);
        when(bandeja.findByIdAndPerfilId(id, perfil)).thenReturn(Optional.of(n));
        when(bandeja.save(n)).thenReturn(n);

        var leida = service.leer(id, perfil);
        assertTrue(leida.leida());
        assertTrue(n.isLeida());

        var n2 = new NotificacionInternaJpaEntity();
        n2.setLeida(false);
        when(bandeja.findByPerfilIdAndLeidaFalse(perfil)).thenReturn(List.of(n2));
        service.leerTodas(perfil);
        assertTrue(n2.isLeida());
        verify(bandeja).saveAll(List.of(n2));
    }

    @Test
    void constructorSinBandejaNotificaDirectamente() {
        var sinBandeja = new NotificacionService(repository, sender);
        UUID perfil = UUID.randomUUID();
        var disp = new DispositivoNotificacionJpaEntity();
        disp.setToken("tok123");
        when(repository.findByPerfilIdAndActivoTrue(perfil)).thenReturn(List.of(disp));

        sinBandeja.notificarPerfil(perfil, "t", "c", Map.of("tipo", "PUSH"));
        verify(sender).enviar(eq("tok123"), eq("t"), eq("c"), any());
    }
}
