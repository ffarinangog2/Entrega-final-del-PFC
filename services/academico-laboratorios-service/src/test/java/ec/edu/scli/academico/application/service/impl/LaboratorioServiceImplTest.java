package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.PisoRepositoryPort;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private LaboratorioServiceImpl laboratorioService;

    @Test
    void obtenerDisponibilidadBase_deberiaRetornarDatosCuandoLaboratorioExiste() {

        UUID id = UUID.randomUUID();

        Laboratorio laboratorio = Laboratorio.nuevo(UUID.randomUUID(), "LAB-001", "Laboratorio de Redes", 30, null);
        laboratorio.setId(id);
        laboratorio.setEstado(EstadoLaboratorio.DISPONIBLE);

        when(laboratorioRepositoryPort.buscarPorId(id)).thenReturn(Optional.of(laboratorio));

        LaboratorioDisponibilidadBaseResponse response =
                laboratorioService.obtenerDisponibilidadBase(id);

        assertThat(response.laboratorioId()).isEqualTo(id);
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
                .isInstanceOf(ec.edu.scli.academico.exception.BusinessRuleException.class);
    }
}
