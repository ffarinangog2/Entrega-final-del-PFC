package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.service.ContextoAcademicoEstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.ContextoEstudianteResumenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextoAcademicoEstudianteControllerTest {

    @Mock
    private ContextoAcademicoEstudianteService service;

    private ContextoAcademicoEstudianteController controller;

    @BeforeEach
    void setUp() {
        controller = new ContextoAcademicoEstudianteController(service);
    }

    @Test
    void listarContextosMasivos_conPeriodoId_deberiaDelegarAlServicio() {
        UUID periodoId = UUID.randomUUID();
        UUID perfilId = UUID.randomUUID();
        UUID estudianteId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        var resumen = new ContextoEstudianteResumenResponse(
                perfilId,
                estudianteId,
                carreraId,
                periodoId,
                2,
                true
        );

        when(service.listarContextosMasivos(periodoId)).thenReturn(List.of(resumen));

        var respuesta = controller.listarContextosMasivos(periodoId);

        assertThat(respuesta).containsExactly(resumen);
        verify(service).listarContextosMasivos(periodoId);
    }

    @Test
    void listarContextosMasivos_sinPeriodoId_deberiaDelegarAlServicioConNull() {
        when(service.listarContextosMasivos(null)).thenReturn(List.of());

        var respuesta = controller.listarContextosMasivos(null);

        assertThat(respuesta).isEmpty();
        verify(service).listarContextosMasivos(null);
    }
}
