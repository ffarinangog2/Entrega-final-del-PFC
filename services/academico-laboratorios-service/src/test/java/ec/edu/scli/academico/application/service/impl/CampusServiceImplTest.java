package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.domain.port.CampusRepositoryPort;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.campus.CampusRequest;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
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
class CampusServiceImplTest {

    @Mock
    private CampusRepositoryPort campusRepositoryPort;

    @InjectMocks
    private CampusServiceImpl campusService;

    private CampusRequest requestValido;

    @BeforeEach
    void configurar() {
        requestValido = new CampusRequest(
                "CENTRAL",
                "Campus Central",
                "Av. Walter Andrade, Quevedo"
        );
    }

    @Test
    void crear_deberiaGuardarCampusCuandoCodigoNoExiste() {

        when(campusRepositoryPort.existeCodigo("CENTRAL")).thenReturn(false);
        when(campusRepositoryPort.guardar(any(Campus.class)))
                .thenAnswer(invocacion -> {
                    Campus c = invocacion.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

        CampusResponse response = campusService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("CENTRAL");
        assertThat(response.nombre()).isEqualTo("Campus Central");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(campusRepositoryPort.existeCodigo("CENTRAL")).thenReturn(true);

        assertThatThrownBy(() -> campusService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CENTRAL");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(campusRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_deberiaRetornarCampusCuandoExiste() {

        UUID id = UUID.randomUUID();
        Campus campusExistente = Campus.nuevo("CENTRAL", "Campus Central", "Direccion");
        campusExistente.setId(id);

        when(campusRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(campusExistente));

        CampusResponse response = campusService.obtenerPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.codigo()).isEqualTo("CENTRAL");
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoCodigoNoEstaDuplicado() {

        UUID id = UUID.randomUUID();
        Campus campusExistente = Campus.nuevo("CENTRAL", "Campus Central", "Direccion vieja");
        campusExistente.setId(id);

        CampusRequest requestActualizado = new CampusRequest(
                "CENTRAL", "Campus Central Renovado", "Direccion nueva");

        when(campusRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(campusExistente));
        when(campusRepositoryPort.existeCodigoParaOtroId("CENTRAL", id)).thenReturn(false);
        when(campusRepositoryPort.guardar(any(Campus.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CampusResponse response = campusService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Campus Central Renovado");
        assertThat(response.direccion()).isEqualTo("Direccion nueva");
    }

    @Test
    void actualizar_deberiaLanzarConflictExceptionCuandoCodigoYaEstaEnOtroCampus() {

        UUID id = UUID.randomUUID();
        Campus campusExistente = Campus.nuevo("CENTRAL", "Campus Central", "Direccion");
        campusExistente.setId(id);

        when(campusRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(campusExistente));
        when(campusRepositoryPort.existeCodigoParaOtroId("CENTRAL", id)).thenReturn(true);

        assertThatThrownBy(() -> campusService.actualizar(id, requestValido))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminar_deberiaDesactivarCampusCuandoExiste() {

        UUID id = UUID.randomUUID();
        Campus campusExistente = Campus.nuevo("CENTRAL", "Campus Central", "Direccion");
        campusExistente.setId(id);

        when(campusRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(campusExistente));
        when(campusRepositoryPort.guardar(any(Campus.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        campusService.eliminar(id);

        assertThat(campusExistente.isActivo()).isFalse();
        verify(campusRepositoryPort).guardar(campusExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(campusRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(campusRepositoryPort, never()).guardar(any(Campus.class));
    }
}