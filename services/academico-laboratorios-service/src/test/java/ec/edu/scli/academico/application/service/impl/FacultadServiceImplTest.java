package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Facultad;
import ec.edu.scli.academico.domain.port.FacultadRepositoryPort;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultadServiceImplTest {

    @Mock
    private FacultadRepositoryPort facultadRepositoryPort;

    @InjectMocks
    private FacultadServiceImpl facultadService;

    private FacultadRequest requestValido;

    @BeforeEach
    void configurar() {
        requestValido = new FacultadRequest(
                "FISEI",
                "Facultad de Ingenieria",
                "Descripcion de prueba"
        );
    }

    @Test
    void crear_deberiaGuardarFacultadCuandoCodigoNoExiste() {

        when(facultadRepositoryPort.existeCodigo("FISEI")).thenReturn(false);
        when(facultadRepositoryPort.guardar(any(Facultad.class)))
                .thenAnswer(invocacion -> {
                    Facultad f = invocacion.getArgument(0);
                    f.setId(UUID.randomUUID());
                    return f;
                });

        FacultadResponse response = facultadService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("FISEI");
        assertThat(response.nombre()).isEqualTo("Facultad de Ingenieria");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(facultadRepositoryPort.existeCodigo("FISEI")).thenReturn(true);

        assertThatThrownBy(() -> facultadService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("FISEI");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(facultadRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultadService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstado_deberiaActualizarActivoCorrectamente() {

        UUID id = UUID.randomUUID();
        Facultad facultadExistente = Facultad.nueva("FISEI", "Facultad de Ingenieria", null);
        facultadExistente.setId(id);

        when(facultadRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(facultadExistente));
        when(facultadRepositoryPort.guardar(any(Facultad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        FacultadResponse response = facultadService.cambiarEstado(id, false);

        assertThat(response.activo()).isFalse();
    }
}
