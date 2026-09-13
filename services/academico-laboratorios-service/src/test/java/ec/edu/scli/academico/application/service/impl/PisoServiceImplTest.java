package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Piso;
import ec.edu.scli.academico.domain.port.BloqueRepositoryPort;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.piso.PisoRequest;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
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
class PisoServiceImplTest {

    @Mock
    private PisoRepositoryPort pisoRepositoryPort;

    @Mock
    private BloqueRepositoryPort bloqueRepositoryPort;

    @InjectMocks
    private PisoServiceImpl pisoService;

    private UUID bloqueId;
    private PisoRequest requestValido;

    @BeforeEach
    void configurar() {
        bloqueId = UUID.randomUUID();
        requestValido = new PisoRequest(bloqueId, 2, "Piso de laboratorios de software");
    }

    @Test
    void crear_deberiaGuardarPisoCuandoBloqueExisteYNumeroNoExiste() {

        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(true);
        when(pisoRepositoryPort.existeNumeroEnBloque(bloqueId, 2)).thenReturn(false);
        when(pisoRepositoryPort.guardar(any(Piso.class)))
                .thenAnswer(invocacion -> {
                    Piso p = invocacion.getArgument(0);
                    p.setId(UUID.randomUUID());
                    return p;
                });

        PisoResponse response = pisoService.crear(requestValido);

        assertThat(response.numero()).isEqualTo(2);
        assertThat(response.bloqueId()).isEqualTo(bloqueId);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoBloqueNoExiste() {

        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(false);

        assertThatThrownBy(() -> pisoService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(bloqueId.toString());

        verify(pisoRepositoryPort, never()).guardar(any(Piso.class));
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoNumeroYaExisteEnElBloque() {

        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(true);
        when(pisoRepositoryPort.existeNumeroEnBloque(bloqueId, 2)).thenReturn(true);

        assertThatThrownBy(() -> pisoService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(pisoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pisoService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_deberiaRetornarPisoCuandoExiste() {

        UUID id = UUID.randomUUID();
        Piso pisoExistente = Piso.nuevo(bloqueId, 2, "Descripcion");
        pisoExistente.setId(id);

        when(pisoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(pisoExistente));

        PisoResponse response = pisoService.obtenerPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.numero()).isEqualTo(2);
    }

    @Test
    void listarPorBloque_deberiaLanzarBusinessRuleExceptionCuandoBloqueNoExiste() {

        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(false);

        assertThatThrownBy(() -> pisoService.listarPorBloque(bloqueId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoBloqueExisteYNumeroNoDuplicado() {

        UUID id = UUID.randomUUID();
        Piso pisoExistente = Piso.nuevo(bloqueId, 2, "Descripcion vieja");
        pisoExistente.setId(id);

        PisoRequest requestActualizado = new PisoRequest(bloqueId, 3, "Descripcion nueva");

        when(pisoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(pisoExistente));
        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(true);
        when(pisoRepositoryPort.existeNumeroEnBloqueParaOtroId(bloqueId, 3, id)).thenReturn(false);
        when(pisoRepositoryPort.guardar(any(Piso.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PisoResponse response = pisoService.actualizar(id, requestActualizado);

        assertThat(response.numero()).isEqualTo(3);
        assertThat(response.descripcion()).isEqualTo("Descripcion nueva");
    }

    @Test
    void actualizar_deberiaLanzarConflictExceptionCuandoNumeroYaEstaEnOtroPisoDelBloque() {

        UUID id = UUID.randomUUID();
        Piso pisoExistente = Piso.nuevo(bloqueId, 2, "Descripcion");
        pisoExistente.setId(id);

        when(pisoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(pisoExistente));
        when(bloqueRepositoryPort.existePorId(bloqueId)).thenReturn(true);
        when(pisoRepositoryPort.existeNumeroEnBloqueParaOtroId(bloqueId, 2, id)).thenReturn(true);

        assertThatThrownBy(() -> pisoService.actualizar(id, requestValido))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminar_deberiaDesactivarPisoCuandoExiste() {

        UUID id = UUID.randomUUID();
        Piso pisoExistente = Piso.nuevo(bloqueId, 2, "Descripcion");
        pisoExistente.setId(id);

        when(pisoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(pisoExistente));
        when(pisoRepositoryPort.guardar(any(Piso.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        pisoService.eliminar(id);

        assertThat(pisoExistente.isActivo()).isFalse();
        verify(pisoRepositoryPort).guardar(pisoExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(pisoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pisoService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pisoRepositoryPort, never()).guardar(any(Piso.class));
    }
}