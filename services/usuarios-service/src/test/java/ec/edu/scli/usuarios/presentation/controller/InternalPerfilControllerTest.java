package ec.edu.scli.usuarios.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.scli.usuarios.application.usecase.ContextoInstitucionalService;
import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.domain.model.ContextoInstitucional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalPerfilControllerTest {
    @Mock PerfilService perfilService;
    @Mock ContextoInstitucionalService contextoService;

    private InternalPerfilController controller;
    private UUID perfilId;

    @BeforeEach
    void setUp() {
        controller = new InternalPerfilController(perfilService, contextoService, "clave-prueba");
        perfilId = UUID.randomUUID();
    }

    @Test
    void contextoRechazaClaveInvalida() {
        var response = controller.obtenerContextoInstitucional(perfilId, "incorrecta");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(contextoService, never()).obtenerPorPerfilId(perfilId);
    }

    @Test
    void contextoAceptaClaveCorrectaYNoExponeDatosPersonales() throws Exception {
        var contexto = new ContextoInstitucional(
                perfilId, true, true, List.of(),
                new ContextoInstitucional.ContextoAdministrador(false, false, null, null, false),
                List.of());
        when(contextoService.obtenerPorPerfilId(perfilId)).thenReturn(contexto);

        var response = controller.obtenerContextoInstitucional(perfilId, "clave-prueba");
        String json = new ObjectMapper().writeValueAsString(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json).doesNotContain(
                "identificacion", "nombres", "apellidos", "email", "telefono", "direccion");
        verify(contextoService).obtenerPorPerfilId(perfilId);
    }
}
