package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Bloque;
import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.domain.port.BloqueRepositoryPort;
import ec.edu.scli.academico.domain.port.CampusRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueServiceImplTest {

    @Mock
    private BloqueRepositoryPort bloqueRepositoryPort;

    @Mock
    private CampusRepositoryPort campusRepositoryPort;

    @InjectMocks
    private BloqueServiceImpl bloqueService;

    private UUID campusId;
    private BloqueRequest requestValido;

    @BeforeEach
    void configurar() {
        campusId = UUID.randomUUID();
        requestValido = new BloqueRequest(campusId, "BLQ-A", "Bloque A");

        lenient().when(campusRepositoryPort.buscarPorId(campusId))
                .thenReturn(Optional.of(new Campus()));
    }

    @Test
    void crear_deberiaGuardarBloqueCuandoCampusExisteYCodigoNoExisteEnEseCampus() {

        when(bloqueRepositoryPort.existeCodigoEnCampus(campusId, "BLQ-A")).thenReturn(false);
        when(bloqueRepositoryPort.guardar(any(Bloque.class)))
                .thenAnswer(invocacion -> {
                    Bloque b = invocacion.getArgument(0);
                    b.setId(UUID.randomUUID());
                    return b;
                });

        BloqueResponse response = bloqueService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("BLQ-A");
        assertThat(response.campusId()).isEqualTo(campusId);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoCampusNoExiste() {

        when(campusRepositoryPort.buscarPorId(campusId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloqueService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(campusId.toString());

        verify(bloqueRepositoryPort, never()).guardar(any(Bloque.class));
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExisteEnEseCampus() {

        when(bloqueRepositoryPort.existeCodigoEnCampus(campusId, "BLQ-A")).thenReturn(true);

        assertThatThrownBy(() -> bloqueService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BLQ-A");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(bloqueRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloqueService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorCampus_deberiaLanzarBusinessRuleExceptionCuandoCampusNoExiste() {

        when(campusRepositoryPort.buscarPorId(campusId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloqueService.listarPorCampus(campusId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoCampusExisteYCodigoNoDuplicado() {

        UUID id = UUID.randomUUID();
        Bloque bloqueExistente = Bloque.nuevo(campusId, "BLQ-A", "Bloque A");
        bloqueExistente.setId(id);

        BloqueRequest requestActualizado = new BloqueRequest(campusId, "BLQ-A", "Bloque A Renovado");

        when(bloqueRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(bloqueExistente));
        when(bloqueRepositoryPort.existeCodigoEnCampusParaOtroId(campusId, "BLQ-A", id)).thenReturn(false);
        when(bloqueRepositoryPort.guardar(any(Bloque.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        BloqueResponse response = bloqueService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Bloque A Renovado");
    }

    @Test
    void eliminar_deberiaDesactivarBloqueCuandoExiste() {

        UUID id = UUID.randomUUID();
        Bloque bloqueExistente = Bloque.nuevo(campusId, "BLQ-A", "Bloque A");
        bloqueExistente.setId(id);

        when(bloqueRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(bloqueExistente));
        when(bloqueRepositoryPort.guardar(any(Bloque.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        bloqueService.eliminar(id);

        assertThat(bloqueExistente.isActivo()).isFalse();
        verify(bloqueRepositoryPort).guardar(bloqueExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(bloqueRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloqueService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bloqueRepositoryPort, never()).guardar(any(Bloque.class));
    }
}