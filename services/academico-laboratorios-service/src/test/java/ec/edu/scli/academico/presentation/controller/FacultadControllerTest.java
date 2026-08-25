package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.FacultadService;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadEstadoRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadRequest;
import ec.edu.scli.academico.presentation.dto.facultad.FacultadResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultadControllerTest {

    @Mock
    private FacultadService facultadService;

    @InjectMocks
    private FacultadController facultadController;

    private FacultadRequest requestValido;
    private FacultadResponse responseEsperado;

    @BeforeEach
    void configurar() {
        requestValido = new FacultadRequest("FISEI", "Facultad de Ingenieria", "Descripcion");
        responseEsperado = new FacultadResponse(
                UUID.randomUUID(), "FISEI", "Facultad de Ingenieria", "Descripcion",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(facultadService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<FacultadResponse> respuesta = facultadController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/facultades/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<FacultadResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(facultadService.listar(eq("FISEI"), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<FacultadResponse>> respuesta =
                facultadController.listar("FISEI", null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConLaFacultad() {

        UUID id = responseEsperado.id();
        when(facultadService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<FacultadResponse> respuesta = facultadController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void actualizar_deberiaRetornar200ConLaFacultadActualizada() {

        UUID id = responseEsperado.id();
        when(facultadService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<FacultadResponse> respuesta = facultadController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void cambiarEstado_deberiaRetornar200ConElEstadoActualizado() {

        UUID id = responseEsperado.id();
        FacultadEstadoRequest estadoRequest = new FacultadEstadoRequest(false);

        FacultadResponse responseDesactivada = new FacultadResponse(
                id, "FISEI", "Facultad de Ingenieria", "Descripcion",
                false, OffsetDateTime.now(), OffsetDateTime.now());

        when(facultadService.cambiarEstado(id, false)).thenReturn(responseDesactivada);

        ResponseEntity<FacultadResponse> respuesta = facultadController.cambiarEstado(id, estadoRequest);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().activo()).isFalse();
    }
}