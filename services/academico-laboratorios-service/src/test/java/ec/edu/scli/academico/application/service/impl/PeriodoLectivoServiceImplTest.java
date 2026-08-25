package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodoLectivoServiceImplTest {

    @Mock
    private PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort;

    @InjectMocks
    private PeriodoLectivoServiceImpl periodoLectivoService;

    private PeriodoLectivoRequest requestValido;

    @BeforeEach
    void configurar() {
        requestValido = new PeriodoLectivoRequest(
                "2026-1",
                "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 7, 31),
                EstadoPeriodo.PLANIFICADO
        );
    }

    @Test
    void crear_deberiaGuardarPeriodoCuandoCodigoNoExisteYFechasValidas() {

        when(periodoLectivoRepositoryPort.existeCodigo("2026-1")).thenReturn(false);
        when(periodoLectivoRepositoryPort.guardar(any(PeriodoLectivo.class)))
                .thenAnswer(invocacion -> {
                    PeriodoLectivo p = invocacion.getArgument(0);
                    p.setId(UUID.randomUUID());
                    return p;
                });

        PeriodoLectivoResponse response = periodoLectivoService.crear(requestValido);

        assertThat(response.codigo()).isEqualTo("2026-1");
        assertThat(response.estado()).isEqualTo(EstadoPeriodo.PLANIFICADO);
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoYaExiste() {

        when(periodoLectivoRepositoryPort.existeCodigo("2026-1")).thenReturn(true);

        assertThatThrownBy(() -> periodoLectivoService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2026-1");
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoFechaFinNoEsPosteriorAFechaInicio() {

        PeriodoLectivoRequest requestFechasInvalidas = new PeriodoLectivoRequest(
                "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 3, 1),
                EstadoPeriodo.PLANIFICADO);

        when(periodoLectivoRepositoryPort.existeCodigo("2026-1")).thenReturn(false);

        assertThatThrownBy(() -> periodoLectivoService.crear(requestFechasInvalidas))
                .isInstanceOf(BusinessRuleException.class);

        verify(periodoLectivoRepositoryPort, never()).guardar(any(PeriodoLectivo.class));
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(periodoLectivoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodoLectivoService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerActual_deberiaRetornarPeriodoCuandoHayUnoActivo() {

        PeriodoLectivo periodoActivo = PeriodoLectivo.nuevo(
                "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.ACTIVO);
        periodoActivo.setId(UUID.randomUUID());

        when(periodoLectivoRepositoryPort.buscarActualPorEstado(EstadoPeriodo.ACTIVO))
                .thenReturn(Optional.of(periodoActivo));

        PeriodoLectivoResponse response = periodoLectivoService.obtenerActual();

        assertThat(response.estado()).isEqualTo(EstadoPeriodo.ACTIVO);
    }

    @Test
    void obtenerActual_deberiaLanzarResourceNotFoundCuandoNoHayPeriodoActivo() {

        when(periodoLectivoRepositoryPort.buscarActualPorEstado(EstadoPeriodo.ACTIVO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodoLectivoService.obtenerActual())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_deberiaActualizarDatosYEstadoCuandoCodigoNoDuplicado() {

        UUID id = UUID.randomUUID();
        PeriodoLectivo periodoExistente = PeriodoLectivo.nuevo(
                "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.PLANIFICADO);
        periodoExistente.setId(id);

        PeriodoLectivoRequest requestActualizado = new PeriodoLectivoRequest(
                "2026-1", "Periodo Regular 2026-1 Renovado",
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 8, 15),
                EstadoPeriodo.ACTIVO);

        when(periodoLectivoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(periodoExistente));
        when(periodoLectivoRepositoryPort.existeCodigoParaOtroId("2026-1", id)).thenReturn(false);
        when(periodoLectivoRepositoryPort.guardar(any(PeriodoLectivo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PeriodoLectivoResponse response = periodoLectivoService.actualizar(id, requestActualizado);

        assertThat(response.nombre()).isEqualTo("Periodo Regular 2026-1 Renovado");
        assertThat(response.estado()).isEqualTo(EstadoPeriodo.ACTIVO);
    }

    @Test
    void verificarExistencia_deberiaRetornarTrueCuandoExiste() {

        UUID id = UUID.randomUUID();
        when(periodoLectivoRepositoryPort.existePorId(id)).thenReturn(true);

        ExisteResponse response = periodoLectivoService.verificarExistencia(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.existe()).isTrue();
    }

    @Test
    void verificarExistencia_deberiaRetornarFalseCuandoNoExiste() {

        UUID id = UUID.randomUUID();
        when(periodoLectivoRepositoryPort.existePorId(id)).thenReturn(false);

        ExisteResponse response = periodoLectivoService.verificarExistencia(id);

        assertThat(response.existe()).isFalse();
    }
}