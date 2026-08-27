package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.audit.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Estas pruebas cubren la lógica más importante para Reservas Service:
 * el endpoint interno de disponibilidad-base. Este microservicio SOLO
 * debe entregar información estructural, nunca decidir disponibilidad
 * por fecha/hora.
 */
@ExtendWith(MockitoExtension.class)
class LaboratorioServiceImplTest {

    @Mock
    private LaboratorioRepositoryPort laboratorioRepositoryPort;

    @Mock
    private PisoRepositoryPort pisoRepositoryPort;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private LaboratorioServiceImpl laboratorioService;

    @Test
    void obtenerDisponibilidadBase_deberiaRetornarDatosCuandoLaboratorioExiste() {

        UUID id = UUID.randomUUID();

        UUID pisoId = UUID.randomUUID();
        Laboratorio laboratorio = Laboratorio.nuevo(pisoId, "LAB-001", "Laboratorio de Redes", 30, null);
        laboratorio.setId(id);
        laboratorio.setEstado(EstadoLaboratorio.DISPONIBLE);

        when(laboratorioRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(laboratorio));

        LaboratorioDisponibilidadBaseResponse response =
                laboratorioService.obtenerDisponibilidadBase(id);

        assertThat(response.laboratorioId()).isEqualTo(id);
        assertThat(response.pisoId()).isEqualTo(pisoId);
        assertThat(response.existe()).isTrue();
        assertThat(response.activo()).isTrue();
        assertThat(response.estado()).isEqualTo(EstadoLaboratorio.DISPONIBLE);
        assertThat(response.capacidad()).isEqualTo(30);
    }

    @Test
    void obtenerDisponibilidadBase_deberiaRetornarExisteFalseCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(laboratorioRepositoryPort.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        LaboratorioDisponibilidadBaseResponse response =
                laboratorioService.obtenerDisponibilidadBase(idInexistente);

        assertThat(response.existe()).isFalse();
        assertThat(response.pisoId()).isNull();
        assertThat(response.activo()).isFalse();
        assertThat(response.estado()).isNull();
        assertThat(response.capacidad()).isNull();
    }

    @Test
    void verificarExistencia_deberiaRetornarTrueCuandoElLaboratorioExiste() {

        UUID id = UUID.randomUUID();

        when(laboratorioRepositoryPort.existePorId(id)).thenReturn(true);

        var response = laboratorioService.verificarExistencia(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.existe()).isTrue();
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoPisoNoExiste() {

        UUID pisoInexistente = UUID.randomUUID();

        when(pisoRepositoryPort.buscarPorId(pisoInexistente)).thenReturn(Optional.empty());

        var request = new ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest(
                pisoInexistente, "LAB-002", "Laboratorio de Software", 25, null
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> laboratorioService.crear(request))
                .isInstanceOf(ec.edu.scli.academico.domain.exception.BusinessRuleException.class);
    }

    @Test
    void crear_deberiaAuditarLaboratorioCreado() {

        UUID pisoId = UUID.randomUUID();
        when(pisoRepositoryPort.buscarPorId(pisoId)).thenReturn(Optional.of(new ec.edu.scli.academico.domain.model.Piso()));
        when(laboratorioRepositoryPort.existeCodigo("LAB-003")).thenReturn(false);
        when(laboratorioRepositoryPort.guardar(org.mockito.ArgumentMatchers.any(Laboratorio.class)))
                .thenAnswer(invocacion -> {
                    Laboratorio laboratorio = invocacion.getArgument(0);
                    laboratorio.setId(UUID.randomUUID());
                    return laboratorio;
                });

        var request = new ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest(
                pisoId, "LAB-003", "Laboratorio de IA", 20, null
        );

        var response = laboratorioService.crear(request);

        verify(auditLogger).registrarEvento(
                eq("laboratorio_creado"), any(), any(), contains("id=" + response.id()));
    }

    @Test
    void cambiarEstado_deberiaAuditarLaboratorioEstadoCambiado() {

        UUID id = UUID.randomUUID();
        UUID pisoId = UUID.randomUUID();
        Laboratorio laboratorio = Laboratorio.nuevo(pisoId, "LAB-004", "Laboratorio de Redes", 30, null);
        laboratorio.setId(id);
        laboratorio.setEstado(EstadoLaboratorio.DISPONIBLE);

        when(laboratorioRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(laboratorio));
        when(laboratorioRepositoryPort.guardar(laboratorio)).thenReturn(laboratorio);

        laboratorioService.cambiarEstado(id, EstadoLaboratorio.MANTENIMIENTO);

        verify(auditLogger).registrarEvento(
                eq("laboratorio_estado_cambiado"), any(), any(),
                org.mockito.ArgumentMatchers.argThat(detalle ->
                        detalle.contains("id=" + id)
                                && detalle.contains("estadoAnterior=DISPONIBLE")
                                && detalle.contains("estadoNuevo=MANTENIMIENTO")));
    }
}
