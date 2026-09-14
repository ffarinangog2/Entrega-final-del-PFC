package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.BloqueService;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.presentation.dto.bloque.BloqueResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueControllerTest {

    @Mock
    private BloqueService bloqueService;

    @InjectMocks
    private BloqueController bloqueController;

    private UUID campusId;
    private BloqueRequest requestValido;
    private BloqueResponse responseEsperado;

    @BeforeEach
    void configurar() {
        campusId = UUID.randomUUID();
        requestValido = new BloqueRequest(campusId, "BLQ-A", "Bloque A");
        responseEsperado = new BloqueResponse(
                UUID.randomUUID(), campusId, "BLQ-A", "Bloque A",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(bloqueService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<BloqueResponse> respuesta = bloqueController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/bloques/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<BloqueResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(bloqueService.listar(eq(campusId), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<BloqueResponse>> respuesta =
                bloqueController.listar(campusId, null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElBloque() {

        UUID id = responseEsperado.id();
        when(bloqueService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<BloqueResponse> respuesta = bloqueController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorCampus_deberiaRetornar200ConLaLista() {

        when(bloqueService.listarPorCampus(campusId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<BloqueResponse>> respuesta = bloqueController.listarPorCampus(campusId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void actualizar_deberiaRetornar200ConElBloqueActualizado() {

        UUID id = responseEsperado.id();
        when(bloqueService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<BloqueResponse> respuesta = bloqueController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = bloqueController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(bloqueService).eliminar(id);
    }
}