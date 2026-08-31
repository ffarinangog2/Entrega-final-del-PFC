package ec.edu.scli.reservas.presentation.controller;

import ec.edu.scli.reservas.application.service.NotificacionService;
import ec.edu.scli.reservas.presentation.dto.request.DesregistrarDispositivoRequest;
import ec.edu.scli.reservas.security.JwtPrincipal;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
