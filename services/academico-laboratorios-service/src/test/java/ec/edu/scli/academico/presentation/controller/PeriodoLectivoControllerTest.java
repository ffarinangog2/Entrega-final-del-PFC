package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.PeriodoLectivoService;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.presentation.dto.periodolectivo.PeriodoLectivoResponse;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodoLectivoControllerTest {

    @Mock
    private PeriodoLectivoService periodoLectivoService;

    @InjectMocks
    private PeriodoLectivoController periodoLectivoController;

    private PeriodoLectivoRequest requestValido;
    private PeriodoLectivoResponse responseEsperado;

    @BeforeEach
    void configurar() {
        requestValido = new PeriodoLectivoRequest(
                "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.PLANIFICADO);

        responseEsperado = new PeriodoLectivoResponse(
                UUID.randomUUID(), "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.PLANIFICADO, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(periodoLectivoService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<PeriodoLectivoResponse> respuesta = periodoLectivoController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/periodos-lectivos/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<PeriodoLectivoResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(periodoLectivoService.listar(eq("2026-1"), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<PeriodoLectivoResponse>> respuesta =
                periodoLectivoController.listar("2026-1", Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerActual_deberiaRetornar200ConElPeriodoActivo() {

        PeriodoLectivoResponse periodoActivo = new PeriodoLectivoResponse(
                UUID.randomUUID(), "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.ACTIVO, OffsetDateTime.now(), OffsetDateTime.now());

        when(periodoLectivoService.obtenerActual()).thenReturn(periodoActivo);

        ResponseEntity<PeriodoLectivoResponse> respuesta = periodoLectivoController.obtenerActual();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoPeriodo.ACTIVO);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElPeriodo() {

        UUID id = responseEsperado.id();
        when(periodoLectivoService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<PeriodoLectivoResponse> respuesta = periodoLectivoController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void actualizar_deberiaRetornar200ConElPeriodoActualizado() {

        UUID id = responseEsperado.id();
        when(periodoLectivoService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<PeriodoLectivoResponse> respuesta =
                periodoLectivoController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }
}