package ec.edu.scli.academico.application.service.impl;

import ec.edu.scli.academico.domain.model.HorarioAcademico;
import ec.edu.scli.academico.domain.port.HorarioAcademicoRepositoryPort;
import ec.edu.scli.academico.domain.port.LaboratorioRepositoryPort;
import ec.edu.scli.academico.domain.port.MateriaRepositoryPort;
import ec.edu.scli.academico.domain.port.PeriodoLectivoRepositoryPort;
import ec.edu.scli.academico.enums.DiaSemana;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;
import ec.edu.scli.academico.domain.exception.ResourceNotFoundException;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
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
class HorarioAcademicoServiceImplTest {

    @Mock
    private HorarioAcademicoRepositoryPort horarioAcademicoRepositoryPort;

    @Mock
    private MateriaRepositoryPort materiaRepositoryPort;

    @Mock
    private PeriodoLectivoRepositoryPort periodoLectivoRepositoryPort;

    @Mock
    private LaboratorioRepositoryPort laboratorioRepositoryPort;

    @InjectMocks
    private HorarioAcademicoServiceImpl horarioService;

    private UUID materiaId;
    private UUID periodoLectivoId;
    private UUID laboratorioId;
    private UUID docenteId;
    private HorarioAcademicoRequest requestValido;

    @BeforeEach
    void configurar() {
        materiaId = UUID.randomUUID();
        periodoLectivoId = UUID.randomUUID();
        laboratorioId = UUID.randomUUID();
        docenteId = UUID.randomUUID();

        requestValido = new HorarioAcademicoRequest(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A"
        );

        lenient().when(materiaRepositoryPort.existePorId(materiaId)).thenReturn(true);
        lenient().when(periodoLectivoRepositoryPort.existePorId(periodoLectivoId)).thenReturn(true);
        lenient().when(laboratorioRepositoryPort.existePorId(laboratorioId)).thenReturn(true);
    }

    @Test
    void crear_deberiaGuardarHorarioCuandoTodoEsValido() {

        when(horarioAcademicoRepositoryPort.guardar(any(HorarioAcademico.class)))
                .thenAnswer(invocacion -> {
                    HorarioAcademico h = invocacion.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });

        HorarioAcademicoResponse response = horarioService.crear(requestValido);

        assertThat(response.materiaId()).isEqualTo(materiaId);
        assertThat(response.diaSemana()).isEqualTo(DiaSemana.LUNES);
        assertThat(response.paralelo()).isEqualTo("A");
        assertThat(response.activo()).isTrue();
    }

    @Test
    void crear_deberiaGuardarHorarioCuandoLaboratorioEsNulo() {

        HorarioAcademicoRequest requestSinLaboratorio = new HorarioAcademicoRequest(
                materiaId, periodoLectivoId, null, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A"
        );

        when(horarioAcademicoRepositoryPort.guardar(any(HorarioAcademico.class)))
                .thenAnswer(invocacion -> {
                    HorarioAcademico h = invocacion.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });

        HorarioAcademicoResponse response = horarioService.crear(requestSinLaboratorio);

        assertThat(response.laboratorioId()).isNull();
        verify(laboratorioRepositoryPort, never()).existePorId(any());
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoMateriaNoExiste() {

        when(materiaRepositoryPort.existePorId(materiaId)).thenReturn(false);

        assertThatThrownBy(() -> horarioService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(materiaId.toString());

        verify(horarioAcademicoRepositoryPort, never()).guardar(any(HorarioAcademico.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoPeriodoLectivoNoExiste() {

        when(periodoLectivoRepositoryPort.existePorId(periodoLectivoId)).thenReturn(false);

        assertThatThrownBy(() -> horarioService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(periodoLectivoId.toString());
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoLaboratorioProporcionadoNoExiste() {

        when(laboratorioRepositoryPort.existePorId(laboratorioId)).thenReturn(false);

        assertThatThrownBy(() -> horarioService.crear(requestValido))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(laboratorioId.toString());
    }

    @Test
    void crear_deberiaLanzarBusinessRuleExceptionCuandoHoraFinNoEsPosteriorAHoraInicio() {

        HorarioAcademicoRequest requestHorasInvalidas = new HorarioAcademicoRequest(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(8, 0), "A"
        );

        assertThatThrownBy(() -> horarioService.crear(requestHorasInvalidas))
                .isInstanceOf(BusinessRuleException.class);

        verify(horarioAcademicoRepositoryPort, never()).guardar(any(HorarioAcademico.class));
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();

        when(horarioAcademicoRepositoryPort.buscarPorId(idInexistente))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> horarioService.obtenerPorId(idInexistente))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_deberiaRetornarListaDeHorarios() {

        HorarioAcademico horario = HorarioAcademico.nuevo(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A");
        horario.setId(UUID.randomUUID());

        when(horarioAcademicoRepositoryPort.buscarTodos()).thenReturn(List.of(horario));

        List<HorarioAcademicoResponse> resultado = horarioService.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).paralelo()).isEqualTo("A");
    }

    @Test
    void listarPorDocente_deberiaRetornarHorariosDelDocente() {

        HorarioAcademico horario = HorarioAcademico.nuevo(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.MARTES, LocalTime.of(10, 0), LocalTime.of(12, 0), "B");
        horario.setId(UUID.randomUUID());

        when(horarioAcademicoRepositoryPort.buscarPorDocente(docenteId)).thenReturn(List.of(horario));

        List<HorarioAcademicoResponse> resultado = horarioService.listarPorDocente(docenteId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).docenteId()).isEqualTo(docenteId);
    }

    @Test
    void listarPorLaboratorio_deberiaRetornarHorariosDelLaboratorio() {

        HorarioAcademico horario = HorarioAcademico.nuevo(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.MIERCOLES, LocalTime.of(14, 0), LocalTime.of(16, 0), "C");
        horario.setId(UUID.randomUUID());

        when(horarioAcademicoRepositoryPort.buscarPorLaboratorio(laboratorioId)).thenReturn(List.of(horario));

        List<HorarioAcademicoResponse> resultado = horarioService.listarPorLaboratorio(laboratorioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).laboratorioId()).isEqualTo(laboratorioId);
    }
}