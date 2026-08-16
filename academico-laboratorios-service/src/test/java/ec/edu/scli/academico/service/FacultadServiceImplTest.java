package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.dto.facultad.FacultadResponse;
import ec.edu.scli.academico.entity.Facultad;
import ec.edu.scli.academico.exception.ConflictException;
import ec.edu.scli.academico.exception.ResourceNotFoundException;
import ec.edu.scli.academico.repository.FacultadRepository;
import ec.edu.scli.academico.service.impl.FacultadServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultadServiceImplTest {

    @Mock
    private FacultadRepository facultadRepository;

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

        when(facultadRepository.existsByCodigo("FISEI")).thenReturn(false);
        when(facultadRepository.save(any(Facultad.class)))
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

        when(facultadRepository.existsByCodigo("FISEI")).thenReturn(true);

        assertThatThrownBy(() -> facultadService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("FISEI");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(facultadRepository.findById(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultadService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstado_deberiaActualizarActivoCorrectamente() {

        UUID id = UUID.randomUUID();
        Facultad facultadExistente = new Facultad();
        facultadExistente.setId(id);
        facultadExistente.setCodigo("FISEI");
        facultadExistente.setNombre("Facultad de Ingenieria");
        facultadExistente.setActivo(true);

        when(facultadRepository.findById(id)).thenReturn(Optional.of(facultadExistente));
        when(facultadRepository.save(any(Facultad.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        FacultadResponse response = facultadService.cambiarEstado(id, false);

        assertThat(response.activo()).isFalse();
    }
}
