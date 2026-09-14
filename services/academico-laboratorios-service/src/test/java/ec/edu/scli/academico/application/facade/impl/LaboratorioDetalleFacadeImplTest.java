package ec.edu.scli.academico.application.facade.impl;

import ec.edu.scli.academico.application.service.BloqueService;
import ec.edu.scli.academico.application.service.CampusService;
import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.PisoService;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioDetalleCompletoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratorioDetalleFacadeImplTest {

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private PisoService pisoService;

    @Mock
    private BloqueService bloqueService;

    @Mock
    private CampusService campusService;

    @Mock
    private EquipoService equipoService;

    @InjectMocks
    private LaboratorioDetalleFacadeImpl laboratorioDetalleFacade;

    private UUID laboratorioId;
    private UUID pisoId;
    private UUID bloqueId;
    private UUID campusId;

    private LaboratorioResponse laboratorioResponse;
    private PisoResponse pisoResponse;
    private BloqueResponse bloqueResponse;
    private CampusResponse campusResponse;
    private EquipoResponse equipoResponse;

    @BeforeEach
    void configurar() {
        laboratorioId = UUID.randomUUID();
        pisoId = UUID.randomUUID();
        bloqueId = UUID.randomUUID();
        campusId = UUID.randomUUID();

        laboratorioResponse = mockLaboratorioResponseConPiso(pisoId);
        pisoResponse = mockPisoResponseConBloque(bloqueId);
        bloqueResponse = mockBloqueResponseConCampus(campusId);
        campusResponse = mock(CampusResponse.class);
        equipoResponse = mock(EquipoResponse.class);
    }

    @Test
    void obtenerDetalleCompleto_deberiaCoordinarLosCincoServiciosEnLaCadenaCorrecta() {

        when(laboratorioService.obtenerPorId(laboratorioId)).thenReturn(laboratorioResponse);
        when(pisoService.obtenerPorId(pisoId)).thenReturn(pisoResponse);
        when(bloqueService.obtenerPorId(bloqueId)).thenReturn(bloqueResponse);
        when(campusService.obtenerPorId(campusId)).thenReturn(campusResponse);
        when(equipoService.listarPorLaboratorio(laboratorioId)).thenReturn(List.of(equipoResponse));

        LaboratorioDetalleCompletoResponse resultado =
                laboratorioDetalleFacade.obtenerDetalleCompleto(laboratorioId);

        assertThat(resultado.laboratorio()).isEqualTo(laboratorioResponse);
        assertThat(resultado.piso()).isEqualTo(pisoResponse);
        assertThat(resultado.bloque()).isEqualTo(bloqueResponse);
        assertThat(resultado.campus()).isEqualTo(campusResponse);
        assertThat(resultado.equipos()).containsExactly(equipoResponse);

        verify(laboratorioService).obtenerPorId(laboratorioId);
        verify(pisoService).obtenerPorId(pisoId);
        verify(bloqueService).obtenerPorId(bloqueId);
        verify(campusService).obtenerPorId(campusId);
        verify(equipoService).listarPorLaboratorio(laboratorioId);
    }

    private LaboratorioResponse mockLaboratorioResponseConPiso(UUID pisoId) {
        LaboratorioResponse response = mock(LaboratorioResponse.class);
        when(response.pisoId()).thenReturn(pisoId);
        return response;
    }

    private PisoResponse mockPisoResponseConBloque(UUID bloqueId) {
        PisoResponse response = mock(PisoResponse.class);
        when(response.bloqueId()).thenReturn(bloqueId);
        return response;
    }

    private BloqueResponse mockBloqueResponseConCampus(UUID campusId) {
        BloqueResponse response = mock(BloqueResponse.class);
        when(response.campusId()).thenReturn(campusId);
        return response;
    }

    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}