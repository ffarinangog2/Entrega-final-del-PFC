package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.NotificacionService;
import ec.edu.scli.reservas.presentation.dto.request.DesregistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.request.RegistrarDispositivoRequest;
import ec.edu.scli.reservas.presentation.dto.response.DispositivoNotificacionResponse;
import ec.edu.scli.reservas.presentation.dto.response.NotificacionInternaResponse;
import ec.edu.scli.reservas.security.JwtPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionControllerTest {

    @Test
    void unregistrationUsesAuthenticatedProfileAndReturnsNoContent() {
        NotificacionService service = mock(NotificacionService.class);
        NotificacionController controller = new NotificacionController(service);
        UUID profileId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(UUID.randomUUID(), profileId, "usuario");

        var response = controller.desregistrar(new DesregistrarDispositivoRequest("token"), principal);

        assertEquals(204, response.getStatusCode().value());
        verify(service).desregistrar("token", profileId);
    }

    @Test
    void handlesAuthenticationPrincipalCorrectly() {
        NotificacionService service = mock(NotificacionService.class);
        NotificacionController controller = new NotificacionController(service);
        UUID authId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(authId, profileId, "usuario");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList());

        Instant now = Instant.now();
        when(service.registrar(new RegistrarDispositivoRequest("token", "ANDROID"), authId, profileId))
                .thenReturn(new DispositivoNotificacionResponse(UUID.randomUUID(), "ANDROID", true, now, now));
        var regResp = controller.registrar(new RegistrarDispositivoRequest("token", "ANDROID"), authentication);
        assertEquals(200, regResp.getStatusCode().value());

        UUID notifId = UUID.randomUUID();
        when(service.listar(profileId)).thenReturn(List.of(new NotificacionInternaResponse(
                notifId, "Titulo", "Cuerpo", "SISTEMA", null, false, now)));
        var listResp = controller.listar(authentication);
        assertEquals(1, listResp.size());

        when(service.noLeidas(profileId)).thenReturn(3L);
        var noLeidasResp = controller.noLeidas(authentication);
        assertEquals(3L, noLeidasResp.get("cantidad"));

        when(service.leer(notifId, profileId)).thenReturn(new NotificacionInternaResponse(
                notifId, "Titulo", "Cuerpo", "SISTEMA", null, true, now));
        var leerResp = controller.leer(notifId, authentication);
        assertEquals(true, leerResp.leida());

        var leerTodasResp = controller.leerTodas(authentication);
        assertEquals(204, leerTodasResp.getStatusCode().value());
        verify(service).leerTodas(profileId);
    }

    @Test
    void rejectsUnsupportedPrincipal() {
        NotificacionService service = mock(NotificacionService.class);
        NotificacionController controller = new NotificacionController(service);
        Principal invalidPrincipal = () -> "anonymous";

        assertThrows(AccessDeniedException.class, () -> controller.listar(invalidPrincipal));
        assertThrows(AccessDeniedException.class, () -> controller.listar(null));
    }
}
