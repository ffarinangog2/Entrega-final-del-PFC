package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.internal.LaboratorioDisponibilidadBaseResponse;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    private static final String API_KEY_VALIDA = "clave-secreta-interna";

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private MateriaService materiaService;

    @Mock
    private PeriodoLectivoService periodoLectivoService;

    private InternalController internalController;

    @BeforeEach
    void configurar() {
        internalController = new InternalController(
                laboratorioService, materiaService, periodoLectivoService, API_KEY_VALIDA);
    }

    @Test
    void disponibilidadBaseLaboratorio_deberiaRetornar200CuandoApiKeyEsValida() {

                UUID id = UUID.randomUUID();
        UUID pisoId = UUID.randomUUID();
        LaboratorioDisponibilidadBaseResponse response = new LaboratorioDisponibilidadBaseResponse(
                id, pisoId, true, true, EstadoLaboratorio.DISPONIBLE, 30);

        when(laboratorioService.obtenerDisponibilidadBase(id)).thenReturn(response);

        ResponseEntity<LaboratorioDisponibilidadBaseResponse> respuesta =
                internalController.disponibilidadBaseLaboratorio(id, API_KEY_VALIDA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(response);
    }

    @Test
    void disponibilidadBaseLaboratorio_deberiaRetornar401CuandoApiKeyEsInvalida() {

        UUID id = UUID.randomUUID();

        ResponseEntity<LaboratorioDisponibilidadBaseResponse> respuesta =
                internalController.disponibilidadBaseLaboratorio(id, "clave-incorrecta");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(laboratorioService, never()).obtenerDisponibilidadBase(id);
    }

    @Test
    void disponibilidadBaseLaboratorio_deberiaRetornar401CuandoApiKeyEsNula() {

        UUID id = UUID.randomUUID();

        ResponseEntity<LaboratorioDisponibilidadBaseResponse> respuesta =
                internalController.disponibilidadBaseLaboratorio(id, null);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(laboratorioService, never()).obtenerDisponibilidadBase(id);
    }

    @Test
    void existeLaboratorio_deberiaRetornar200CuandoApiKeyEsValida() {

        UUID id = UUID.randomUUID();
        ExisteResponse response = new ExisteResponse(id, true);

        when(laboratorioService.verificarExistencia(id)).thenReturn(response);

        ResponseEntity<ExisteResponse> respuesta =
                internalController.existeLaboratorio(id, API_KEY_VALIDA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().existe()).isTrue();
    }

    @Test
    void existeLaboratorio_deberiaRetornar401CuandoApiKeyEsInvalida() {

        UUID id = UUID.randomUUID();

        ResponseEntity<ExisteResponse> respuesta =
                internalController.existeLaboratorio(id, "clave-incorrecta");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void existeMateria_deberiaRetornar200CuandoApiKeyEsValida() {

        UUID id = UUID.randomUUID();
        ExisteResponse response = new ExisteResponse(id, true);

        when(materiaService.verificarExistencia(id)).thenReturn(response);

        ResponseEntity<ExisteResponse> respuesta = internalController.existeMateria(id, API_KEY_VALIDA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().existe()).isTrue();
    }

    @Test
    void existeMateria_deberiaRetornar401CuandoApiKeyEsInvalida() {

        UUID id = UUID.randomUUID();

        ResponseEntity<ExisteResponse> respuesta = internalController.existeMateria(id, "clave-incorrecta");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(materiaService, never()).verificarExistencia(id);
    }

    @Test
    void existePeriodoLectivo_deberiaRetornar200CuandoApiKeyEsValida() {

        UUID id = UUID.randomUUID();
        ExisteResponse response = new ExisteResponse(id, false);

        when(periodoLectivoService.verificarExistencia(id)).thenReturn(response);

        ResponseEntity<ExisteResponse> respuesta =
                internalController.existePeriodoLectivo(id, API_KEY_VALIDA);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().existe()).isFalse();
    }

    @Test
    void existePeriodoLectivo_deberiaRetornar401CuandoApiKeyEsInvalida() {

        UUID id = UUID.randomUUID();

        ResponseEntity<ExisteResponse> respuesta =
                internalController.existePeriodoLectivo(id, "clave-incorrecta");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(periodoLectivoService, never()).verificarExistencia(id);
    }
}