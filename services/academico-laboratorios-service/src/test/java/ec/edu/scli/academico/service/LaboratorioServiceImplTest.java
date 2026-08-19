package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.entity.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.repository.LaboratorioRepository;
import ec.edu.scli.academico.repository.PisoRepository;
import ec.edu.scli.academico.service.impl.LaboratorioServiceImpl;
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
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private PisoRepository pisoRepository;

    @InjectMocks
    private LaboratorioServiceImpl laboratorioService;

    @Test
    void obtenerDisponibilidadBase_deberiaRetornarDatosCuandoLaboratorioExiste() {

        UUID id = UUID.randomUUID();

        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setId(id);
        laboratorio.setCodigo("LAB-001");
        laboratorio.setNombre("Laboratorio de Redes");
        laboratorio.setCapacidad(30);
        laboratorio.setEstado(EstadoLaboratorio.DISPONIBLE);
        laboratorio.setActivo(true);

        when(laboratorioRepository.findById(id)).thenReturn(Optional.of(laboratorio));

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

        when(laboratorioRepository.findById(idInexistente)).thenReturn(Optional.empty());

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

        when(laboratorioRepository.existsById(id)).thenReturn(true);

        var response = laboratorioService.verificarExistencia(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.existe()).isTrue();
    }
}
