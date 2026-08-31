package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.facade.LaboratorioDetalleFacade;
import ec.edu.scli.academico.application.service.LaboratorioService;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.observability.PrometheusQueryClient;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioDetalleCompletoResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioEstadoRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioRequest;
import ec.edu.scli.academico.presentation.dto.laboratorio.LaboratorioResponse;
import ec.edu.scli.academico.presentation.dto.laboratorio.SerieEstadoResponse;
import ec.edu.scli.academico.security.PoliticaAmbitoAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratorioControllerTest {

    @Mock
    private LaboratorioService laboratorioService;

    @Mock
    private LaboratorioDetalleFacade laboratorioDetalleFacade;

    @Mock
    private PrometheusQueryClient prometheusQueryClient;

    @Mock
    private PoliticaAmbitoAcademico politicaAmbitoAcademico;

    @InjectMocks
    private LaboratorioController laboratorioController;

    private LaboratorioRequest requestValido;
    private LaboratorioResponse responseEsperado;

    @BeforeEach
    void configurar() {
        UUID pisoId = UUID.randomUUID();
        requestValido = new LaboratorioRequest(pisoId, "LAB-01", "Laboratorio de Software", 30, "Descripcion");
        responseEsperado = new LaboratorioResponse(
                UUID.randomUUID(), pisoId, "LAB-01", "Laboratorio de Software", 30, "Descripcion",
                EstadoLaboratorio.DISPONIBLE, true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(laboratorioService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<LaboratorioResponse> respuesta = laboratorioController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/laboratorios/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<LaboratorioResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(laboratorioService.listar(eq("Lab"), eq(EstadoLaboratorio.DISPONIBLE), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<LaboratorioResponse>> respuesta = laboratorioController.listar(
                "Lab", EstadoLaboratorio.DISPONIBLE, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void listarDisponibles_deberiaRetornar200ConLaLista() {

        when(laboratorioService.listarDisponibles()).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<LaboratorioResponse>> respuesta = laboratorioController.listarDisponibles();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerOcupacionHistorica_deberiaRetornar200ConLaSerie() {

        SerieEstadoResponse serieMock = mock(SerieEstadoResponse.class);
        when(prometheusQueryClient.consultarOcupacionHistorica(60)).thenReturn(List.of(serieMock));

        ResponseEntity<List<SerieEstadoResponse>> respuesta =
                laboratorioController.obtenerOcupacionHistorica(60);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElLaboratorio() {

        UUID id = responseEsperado.id();
        when(laboratorioService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<LaboratorioResponse> respuesta = laboratorioController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void obtenerDetalleCompleto_deberiaRetornar200ConElDetalleDelFacade() {

        UUID id = responseEsperado.id();
        LaboratorioDetalleCompletoResponse detalleMock = mock(LaboratorioDetalleCompletoResponse.class);

        when(laboratorioDetalleFacade.obtenerDetalleCompleto(id)).thenReturn(detalleMock);

        ResponseEntity<LaboratorioDetalleCompletoResponse> respuesta =
                laboratorioController.obtenerDetalleCompleto(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(detalleMock);
    }

    @Test
    void actualizar_deberiaRetornar200ConElLaboratorioActualizado() {

        UUID id = responseEsperado.id();
        when(laboratorioService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<LaboratorioResponse> respuesta = laboratorioController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void cambiarEstado_deberiaRetornar200ConElEstadoActualizado() {

        UUID id = responseEsperado.id();
        LaboratorioEstadoRequest estadoRequest = new LaboratorioEstadoRequest(EstadoLaboratorio.MANTENIMIENTO);

        LaboratorioResponse responseEnMantenimiento = new LaboratorioResponse(
                id, responseEsperado.pisoId(), "LAB-01", "Laboratorio de Software", 30, "Descripcion",
                EstadoLaboratorio.MANTENIMIENTO, true, OffsetDateTime.now(), OffsetDateTime.now());

        when(laboratorioService.cambiarEstado(id, EstadoLaboratorio.MANTENIMIENTO))
                .thenReturn(responseEnMantenimiento);

        ResponseEntity<LaboratorioResponse> respuesta =
                laboratorioController.cambiarEstado(id, estadoRequest);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoLaboratorio.MANTENIMIENTO);
    }
}
