package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.HorarioAcademicoService;
import ec.edu.scli.academico.enums.DiaSemana;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoRequest;
import ec.edu.scli.academico.presentation.dto.horario.HorarioAcademicoResponse;
import ec.edu.scli.academico.security.PoliticaAmbitoAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioAcademicoControllerTest {

    @Mock
    private HorarioAcademicoService horarioAcademicoService;

    @Mock
    private PoliticaAmbitoAcademico politicaAmbitoAcademico;

    @InjectMocks
    private HorarioAcademicoController horarioAcademicoController;

    private UUID docenteId;
    private UUID laboratorioId;
    private HorarioAcademicoRequest requestValido;
    private HorarioAcademicoResponse responseEsperado;

    @BeforeEach
    void configurar() {
        lenient().when(politicaAmbitoAcademico.filtrarHorariosLectura(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        docenteId = UUID.randomUUID();
        laboratorioId = UUID.randomUUID();
        UUID materiaId = UUID.randomUUID();
        UUID periodoLectivoId = UUID.randomUUID();

        requestValido = new HorarioAcademicoRequest(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A");

        responseEsperado = new HorarioAcademicoResponse(
                UUID.randomUUID(), materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(horarioAcademicoService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<HorarioAcademicoResponse> respuesta =
                horarioAcademicoController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/horarios/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConLaListaCompleta() {

        when(horarioAcademicoService.listar()).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<HorarioAcademicoResponse>> respuesta = horarioAcademicoController.listar();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElHorario() {

        UUID id = responseEsperado.id();
        when(horarioAcademicoService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<HorarioAcademicoResponse> respuesta = horarioAcademicoController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorDocente_deberiaRetornar200ConLosHorariosDelDocente() {

        when(horarioAcademicoService.listarPorDocente(docenteId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<HorarioAcademicoResponse>> respuesta =
                horarioAcademicoController.listarPorDocente(docenteId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void listarPorLaboratorio_deberiaRetornar200ConLosHorariosDelLaboratorio() {

        when(horarioAcademicoService.listarPorLaboratorio(laboratorioId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<HorarioAcademicoResponse>> respuesta =
                horarioAcademicoController.listarPorLaboratorio(laboratorioId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }
}
