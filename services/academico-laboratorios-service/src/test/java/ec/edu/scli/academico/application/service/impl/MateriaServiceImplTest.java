package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.domain.model.Materia;
import ec.edu.scli.academico.domain.port.CarreraRepositoryPort;
import ec.edu.scli.academico.domain.port.MateriaRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.materia.MateriaRequest;
import ec.edu.scli.academico.presentation.dto.materia.MateriaResponse;
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
class MateriaServiceImplTest {

    @Mock
    private MateriaRepositoryPort materiaRepositoryPort;

    @Mock
    private CarreraRepositoryPort carreraRepositoryPort;

    @InjectMocks
    private MateriaServiceImpl materiaService;

    private UUID carreraId;
    private MateriaRequest requestValido;

    @BeforeEach
    void configurar() {
        carreraId = UUID.randomUUID();
        requestValido = new MateriaRequest(carreraId, "PROG1", "Programacion I", 64);

        lenient().when(carreraRepositoryPort.buscarPorId(carreraId))
                .thenReturn(Optional.of(new Carrera()));
    }

    @Test
    void crear_deberiaGuardarMateriaCuandoCarreraExisteYCodigoNoExiste() {

        when(materiaRepositoryPort.existeCodigo("PROG1")).thenReturn(false);
        when(materiaRepositoryPort.guardar(any(Materia.class)))
                .thenAnswer(invocacion -> {
                    Materia m = invocacion.getArgument(0);
                    m.setId(UUID.randomUUID());
                    return m;
                });

        MateriaResponse response = materiaService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("PROG1");
        assertThat(response.numeroHoras()).isEqualTo(64);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoCarreraNoExiste() {

        when(carreraRepositoryPort.buscarPorId(carreraId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(carreraId.toString());

        verify(materiaRepositoryPort, never()).guardar(any(Materia.class));
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(materiaRepositoryPort.existeCodigo("PROG1")).thenReturn(true);

        assertThatThrownBy(() -> materiaService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PROG1");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(materiaRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorCarrera_deberiaLanzarBusinessRuleExceptionCuandoCarreraNoExiste() {

        when(carreraRepositoryPort.buscarPorId(carreraId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.listarPorCarrera(carreraId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void actualizar_deberiaActualizarDatosCuandoCarreraExisteYCodigoNoDuplicado() {

        UUID id = UUID.randomUUID();
        Materia materiaExistente = Materia.nueva(carreraId, "PROG1", "Programacion I", 64);
        materiaExistente.setId(id);

        MateriaRequest requestActualizado = new MateriaRequest(carreraId, "PROG1", "Programacion I Avanzada", 80);

        when(materiaRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(materiaExistente));
        when(materiaRepositoryPort.existeCodigoParaOtroId("PROG1", id)).thenReturn(false);
        when(materiaRepositoryPort.guardar(any(Materia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        MateriaResponse response = materiaService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Programacion I Avanzada");
        assertThat(response.numeroHoras()).isEqualTo(80);
    }

    @Test
    void eliminar_deberiaDesactivarMateriaCuandoExiste() {

        UUID id = UUID.randomUUID();
        Materia materiaExistente = Materia.nueva(carreraId, "PROG1", "Programacion I", 64);
        materiaExistente.setId(id);

        when(materiaRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(materiaExistente));
        when(materiaRepositoryPort.guardar(any(Materia.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        materiaService.eliminar(id);

        assertThat(materiaExistente.isActivo()).isFalse();
        verify(materiaRepositoryPort).guardar(materiaExistente);
    }

    @Test
    void eliminar_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(materiaRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.eliminar(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(materiaRepositoryPort, never()).guardar(any(Materia.class));
    }

    @Test
    void verificarExistencia_deberiaRetornarTrueCuandoExiste() {

        UUID id = UUID.randomUUID();
        when(materiaRepositoryPort.existePorId(id)).thenReturn(true);

        ExisteResponse response = materiaService.verificarExistencia(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.existe()).isTrue();
    }

    @Test
    void verificarExistencia_deberiaRetornarFalseCuandoNoExiste() {

        UUID id = UUID.randomUUID();
        when(materiaRepositoryPort.existePorId(id)).thenReturn(false);

        ExisteResponse response = materiaService.verificarExistencia(id);

        assertThat(response.existe()).isFalse();
    }
}