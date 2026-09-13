package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.CampusService;
import ec.edu.scli.academico.presentation.dto.campus.CampusRequest;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusControllerTest {

    @Mock
    private CampusService campusService;

    @InjectMocks
    private CampusController campusController;

    private CampusRequest requestValido;
    private CampusResponse responseEsperado;

    @BeforeEach
    void configurar() {
        requestValido = new CampusRequest("CENTRAL", "Campus Central", "Direccion");
        responseEsperado = new CampusResponse(
                UUID.randomUUID(), "CENTRAL", "Campus Central", "Direccion",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(campusService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<CampusResponse> respuesta = campusController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/campus/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<CampusResponse> pagina = new PageImpl<>(java.util.List.of(responseEsperado));

        when(campusService.listar(eq("CENTRAL"), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<CampusResponse>> respuesta =
                campusController.listar("CENTRAL", null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElCampus() {

        UUID id = responseEsperado.id();
        when(campusService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<CampusResponse> respuesta = campusController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void actualizar_deberiaRetornar200ConElCampusActualizado() {

        UUID id = responseEsperado.id();
        when(campusService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<CampusResponse> respuesta = campusController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = campusController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(campusService).eliminar(id);
    }
}