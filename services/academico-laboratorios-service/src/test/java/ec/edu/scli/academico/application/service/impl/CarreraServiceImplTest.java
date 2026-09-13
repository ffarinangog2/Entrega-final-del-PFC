package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.domain.port.CarreraRepositoryPort;
import ec.edu.scli.academico.domain.port.FacultadRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarreraServiceImplTest {

    @Mock
    private CarreraRepositoryPort carreraRepositoryPort;

    @Mock
    private FacultadRepositoryPort facultadRepositoryPort;

    @InjectMocks
    private CarreraServiceImpl carreraService;

    private UUID facultadId;
    private CarreraRequest requestValido;

    @BeforeEach
    void configurar() {
        facultadId = UUID.randomUUID();
        requestValido = new CarreraRequest(
                facultadId,
                "SOFT",
                "Ingenieria de Software",
                "Descripcion de prueba"
        );
    }

    @Test
    void crear_deberiaGuardarCarreraCuandoFacultadExisteYCodigoNoExiste() {

        when(facultadRepositoryPort.existePorId(facultadId)).thenReturn(true);
        when(carreraRepositoryPort.existeCodigo("SOFT")).thenReturn(false);
        when(carreraRepositoryPort.guardar(any(Carrera.class)))
                .thenAnswer(invocacion -> {
                    Carrera c = invocacion.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

        CarreraResponse response = carreraService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("SOFT");
        assertThat(response.facultadId()).isEqualTo(facultadId);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoFacultadNoExiste() {

        when(facultadRepositoryPort.existePorId(facultadId)).thenReturn(false);

        assertThatThrownBy(() -> carreraService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(facultadId.toString());

        verify(carreraRepositoryPort, never()).guardar(any(Carrera.class));
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(facultadRepositoryPort.existePorId(facultadId)).thenReturn(true);
        when(carreraRepositoryPort.existeCodigo("SOFT")).thenReturn(true);

        assertThatThrownBy(() -> carreraService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SOFT");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(carreraRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> carreraService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_deberiaRetornarCarreraCuandoExiste() {

        UUID id = UUID.randomUUID();
        Carrera carreraExistente = Carrera.nueva(facultadId, "SOFT", "Ingenieria de Software", "Desc");
        carreraExistente.setId(id);

        when(carreraRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(carreraExistente));

        CarreraResponse response = carreraService.obtenerPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.codigo()).isEqualTo("SOFT");
    }

    @Test
    void listarPorFacultad_deberiaLanzarBusinessRuleExceptionCuandoFacultadNoExiste() {

        when(facultadRepositoryPort.existePorId(facultadId)).thenReturn(false);

        assertThatThrownBy(() -> carreraService.listarPorFacultad(facultadId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoFacultadExisteYCodigoNoDuplicado() {

        UUID id = UUID.randomUUID();
        Carrera carreraExistente = Carrera.nueva(facultadId, "SOFT", "Ingenieria de Software", "Desc vieja");
        carreraExistente.setId(id);

        CarreraRequest requestActualizado = new CarreraRequest(
                facultadId, "SOFT", "Ingenieria de Software Renovada", "Desc nueva");

        when(carreraRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(carreraExistente));
        when(facultadRepositoryPort.existePorId(facultadId)).thenReturn(true);
        when(carreraRepositoryPort.existeCodigoParaOtroId("SOFT", id)).thenReturn(false);
        when(carreraRepositoryPort.guardar(any(Carrera.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CarreraResponse response = carreraService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Ingenieria de Software Renovada");
        assertThat(response.descripcion()).isEqualTo("Desc nueva");
    }

    @Test
    void eliminar_deberiaDesactivarCarreraCuandoExiste() {

        UUID id = UUID.randomUUID();
        Carrera carreraExistente = Carrera.nueva(facultadId, "SOFT", "Ingenieria de Software", "Desc");
        carreraExistente.setId(id);

        when(carreraRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(carreraExistente));
        when(carreraRepositoryPort.guardar(any(Carrera.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        carreraService.eliminar(id);

        assertThat(carreraExistente.isActivo()).isFalse();
        verify(carreraRepositoryPort).guardar(carreraExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(carreraRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> carreraService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(carreraRepositoryPort, never()).guardar(any(Carrera.class));
    }
}