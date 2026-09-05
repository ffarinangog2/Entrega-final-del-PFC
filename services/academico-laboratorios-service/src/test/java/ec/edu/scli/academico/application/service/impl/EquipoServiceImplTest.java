package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.EquipoRepositoryPort;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.TipoEquipoRepositoryPort;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ConflictException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.infrastructure.audit.AuditLogger;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceImplTest {

    @Mock
    private EquipoRepositoryPort equipoRepositoryPort;

    @Mock
    private LaboratorioRepositoryPort laboratorioRepositoryPort;

    @Mock
    private TipoEquipoRepositoryPort tipoEquipoRepositoryPort;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private EquipoServiceImpl equipoService;

    private UUID laboratorioId;
    private UUID tipoEquipoId;
    private EquipoRequest requestValido;

    @BeforeEach
    void configurar() {
        laboratorioId = UUID.randomUUID();
        tipoEquipoId = UUID.randomUUID();
        requestValido = new EquipoRequest(
                laboratorioId,
                tipoEquipoId,
                "INV-001",
                "SN-001",
                "Dell",
                "OptiPlex",
                "i7",
                "16GB",
                "512GB",
                "192.168.1.10",
                "AA:BB:CC:DD:EE:FF",
                "Sin observaciones"
        );

        lenient().when(laboratorioRepositoryPort.buscarPorId(laboratorioId))
                .thenReturn(Optional.of(new Laboratorio()));
        lenient().when(tipoEquipoRepositoryPort.existePorId(tipoEquipoId)).thenReturn(true);
    }

    @Test
    void crear_deberiaGuardarEquipoCuandoTodoEsValido() {

        when(equipoRepositoryPort.existeCodigoInventario("INV-001")).thenReturn(false);
        when(equipoRepositoryPort.existeNumeroSerie("SN-001")).thenReturn(false);
        when(equipoRepositoryPort.guardar(any(Equipo.class)))
                .thenAnswer(invocacion -> {
                    Equipo e = invocacion.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        EquipoResponse response = equipoService.crear(requestValido);

        assertThat(response.codigoInventario()).isEqualTo("INV-001");
        assertThat(response.estado()).isEqualTo(EstadoEquipo.OPERATIVO);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaAuditarEquipoCreado() {

        when(equipoRepositoryPort.existeCodigoInventario("INV-001")).thenReturn(false);
        when(equipoRepositoryPort.existeNumeroSerie("SN-001")).thenReturn(false);
        when(equipoRepositoryPort.guardar(any(Equipo.class)))
                .thenAnswer(invocacion -> {
                    Equipo e = invocacion.getArgument(0);
                    e.setId(UUID.randomUUID());
                    return e;
                });

        EquipoResponse response = equipoService.crear(requestValido);

        verify(auditLogger).registrarEvento(
                eq("equipo_creado"), any(), any(), contains("id=" + response.id()));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoLaboratorioNoExiste() {

        when(laboratorioRepositoryPort.buscarPorId(laboratorioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(laboratorioId.toString());

        verify(equipoRepositoryPort, never()).guardar(any(Equipo.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoTipoEquipoNoExiste() {

        when(tipoEquipoRepositoryPort.existePorId(tipoEquipoId)).thenReturn(false);

        assertThatThrownBy(() -> equipoService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(tipoEquipoId.toString());

        verify(equipoRepositoryPort, never()).guardar(any(Equipo.class));
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoCodigoInventarioYaExiste() {

        when(equipoRepositoryPort.existeCodigoInventario("INV-001")).thenReturn(true);

        assertThatThrownBy(() -> equipoService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INV-001");
    }

    @Test
    void crear_deberiaLanzarConflictExceptionCuandoNumeroSerieYaExiste() {

        when(equipoRepositoryPort.existeCodigoInventario("INV-001")).thenReturn(false);
        when(equipoRepositoryPort.existeNumeroSerie("SN-001")).thenReturn(true);

        assertThatThrownBy(() -> equipoService.crear(requestValido))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SN-001");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(equipoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorLaboratorio_deberiaLanzarBusinessRuleExceptionCuandoLaboratorioNoExiste() {

        when(laboratorioRepositoryPort.buscarPorId(laboratorioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.listarPorLaboratorio(laboratorioId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cambiarEstado_deberiaDesactivarEquipoCuandoPasaAFueraDeServicio() {

        UUID id = UUID.randomUUID();
        Equipo equipoExistente = Equipo.nuevo(
                laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF", "Sin observaciones");
        equipoExistente.setId(id);

        when(equipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(equipoExistente));
        when(equipoRepositoryPort.guardar(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EquipoResponse response = equipoService.cambiarEstado(id, EstadoEquipo.FUERA_DE_SERVICIO);

        assertThat(response.estado()).isEqualTo(EstadoEquipo.FUERA_DE_SERVICIO);
        assertThat(response.activo()).isFalse();
    }

    @Test
    void cambiarEstado_deberiaReactivarEquipoCuandoVuelveAOperativo() {

        UUID id = UUID.randomUUID();
        Equipo equipoExistente = Equipo.nuevo(
                laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF", "Sin observaciones");
        equipoExistente.setId(id);
        equipoExistente.cambiarEstado(EstadoEquipo.FUERA_DE_SERVICIO);

        when(equipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(equipoExistente));
        when(equipoRepositoryPort.guardar(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EquipoResponse response = equipoService.cambiarEstado(id, EstadoEquipo.OPERATIVO);

        assertThat(response.estado()).isEqualTo(EstadoEquipo.OPERATIVO);
        assertThat(response.activo()).isTrue();
    }

    @Test
    void cambiarEstado_deberiaAuditarEquipoEstadoCambiado() {

        UUID id = UUID.randomUUID();
        Equipo equipoExistente = Equipo.nuevo(
                laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF", "Sin observaciones");
        equipoExistente.setId(id);

        when(equipoRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(equipoExistente));
        when(equipoRepositoryPort.guardar(any(Equipo.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        equipoService.cambiarEstado(id, EstadoEquipo.FUERA_DE_SERVICIO);

        verify(auditLogger).registrarEvento(
                eq("equipo_estado_cambiado"), any(), any(),
                org.mockito.ArgumentMatchers.argThat(detalle ->
                        detalle.contains("id=" + id)
                                && detalle.contains("estadoAnterior=OPERATIVO")
                                && detalle.contains("estadoNuevo=FUERA_DE_SERVICIO")));
    }
}