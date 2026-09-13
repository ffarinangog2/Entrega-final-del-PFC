package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.DocenteService;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalDocenteControllerTest {
    @Test
    void resuelveDocenteRealDesdePerfilConClaveInterna() {
        DocenteService service = mock(DocenteService.class);
        UUID perfilId = UUID.randomUUID();
        UUID docenteId = UUID.randomUUID();
        when(service.obtenerPorPerfilId(perfilId)).thenReturn(
                new DocenteResponse(docenteId, perfilId, "DOC-1", null, null, null, null,
                        true, null, null));
        var controller = new InternalDocenteController(service, "clave");
        var response = controller.obtenerPorPerfilId(perfilId, "clave");
        assertThat(response.getBody().docenteId()).isEqualTo(docenteId);
        assertThat(response.getBody().perfilId()).isEqualTo(perfilId);
        assertThat(response.getBody().activo()).isTrue();
    }

    @Test
    void rechazaClaveInvalidaSinConsultarDocente() {
        DocenteService service = mock(DocenteService.class);
        UUID perfilId = UUID.randomUUID();
        var response = new InternalDocenteController(service, "clave")
                .obtenerPorPerfilId(perfilId, "incorrecta");
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(service);
    }

    @Test
    void rechazaClaveAusenteSinConsultarDocente() {
        DocenteService service = mock(DocenteService.class);
        var response = new InternalDocenteController(service, "clave")
                .obtenerPorPerfilId(UUID.randomUUID(), null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(service);
    }
}
