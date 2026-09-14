package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.TipoEquipo;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoResponse;
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
class TipoEquipoServiceImplTest {

    @Mock
    private TipoEquipoRepositoryPort tipoEquipoRepositoryPort;

    @InjectMocks
    private TipoEquipoServiceImpl tipoEquipoService;

    private TipoEquipoRequest requestValido;

    @BeforeEach
    void configurar() {
        requestValido = new TipoEquipoRequest(
                "PC-DESK",
                "Computador de escritorio",
                "Descripcion de prueba"
        );
    }

    @Test
    void crear_deberiaGuardarTipoEquipoCuandoCodigoNoExiste() {

        when(tipoEquipoRepositoryPort.existeCodigo("PC-DESK")).thenReturn(false);
        when(tipoEquipoRepositoryPort.guardar(any(TipoEquipo.class)))
                .thenAnswer(invocacion -> {
                    TipoEquipo t = invocacion.getArgument(0);
                    t.setId(UUID.randomUUID());
                    return t;
                });

        TipoEquipoResponse response = tipoEquipoService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("PC-DESK");
        assertThat(response.nombre()).isEqualTo("Computador de escritorio");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(tipoEquipoRepositoryPort.existeCodigo("PC-DESK")).thenReturn(true);

        assertThatThrownBy(() -> tipoEquipoService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PC-DESK");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(tipoEquipoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoEquipoService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_deberiaRetornarTipoEquipoCuandoExiste() {

        UUID id = UUID.randomUUID();
        TipoEquipo tipoExistente = TipoEquipo.nuevo("PC-DESK", "Computador de escritorio", "Desc");
        tipoExistente.setId(id);

        when(tipoEquipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(tipoExistente));

        TipoEquipoResponse response = tipoEquipoService.obtenerPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.codigo()).isEqualTo("PC-DESK");
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoCodigoNoEstaDuplicado() {

        UUID id = UUID.randomUUID();
        TipoEquipo tipoExistente = TipoEquipo.nuevo("PC-DESK", "Computador de escritorio", "Desc vieja");
        tipoExistente.setId(id);

        TipoEquipoRequest requestActualizado = new TipoEquipoRequest(
                "PC-DESK", "Computador de escritorio Pro", "Desc nueva");

        when(tipoEquipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(tipoExistente));
        when(tipoEquipoRepositoryPort.existeCodigoParaOtroId("PC-DESK", id)).thenReturn(false);
        when(tipoEquipoRepositoryPort.guardar(any(TipoEquipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        TipoEquipoResponse response = tipoEquipoService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Computador de escritorio Pro");
        assertThat(response.descripcion()).isEqualTo("Desc nueva");
    }

    @Test
    void actualizar_deberiaLanzarConflictExceptionCuandoCodigoYaEstaEnOtroTipoEquipo() {

        UUID id = UUID.randomUUID();
        TipoEquipo tipoExistente = TipoEquipo.nuevo("PC-DESK", "Computador de escritorio", "Desc");
        tipoExistente.setId(id);

        when(tipoEquipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(tipoExistente));
        when(tipoEquipoRepositoryPort.existeCodigoParaOtroId("PC-DESK", id)).thenReturn(true);

        assertThatThrownBy(() -> tipoEquipoService.actualizar(id, requestValido))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminar_deberiaDesactivarTipoEquipoCuandoExiste() {

        UUID id = UUID.randomUUID();
        TipoEquipo tipoExistente = TipoEquipo.nuevo("PC-DESK", "Computador de escritorio", "Desc");
        tipoExistente.setId(id);

        when(tipoEquipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(tipoExistente));
        when(tipoEquipoRepositoryPort.guardar(any(TipoEquipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        tipoEquipoService.eliminar(id);

        assertThat(tipoExistente.isActivo()).isFalse();
        verify(tipoEquipoRepositoryPort).guardar(tipoExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(tipoEquipoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoEquipoService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tipoEquipoRepositoryPort, never()).guardar(any(TipoEquipo.class));
    }
}