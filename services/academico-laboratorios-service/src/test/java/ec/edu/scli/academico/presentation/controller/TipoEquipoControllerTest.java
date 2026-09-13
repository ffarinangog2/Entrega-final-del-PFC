package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.TipoEquipoService;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.presentation.dto.tipoequipo.TipoEquipoResponse;
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
class TipoEquipoControllerTest {

    @Mock
    private TipoEquipoService tipoEquipoService;

    @InjectMocks
    private TipoEquipoController tipoEquipoController;

    private TipoEquipoRequest requestValido;
    private TipoEquipoResponse responseEsperado;

    @BeforeEach
    void configurar() {
        requestValido = new TipoEquipoRequest("PC-DESK", "Computador de escritorio", "Descripcion");
        responseEsperado = new TipoEquipoResponse(
                UUID.randomUUID(), "PC-DESK", "Computador de escritorio", "Descripcion",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(tipoEquipoService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<TipoEquipoResponse> respuesta = tipoEquipoController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/tipos-equipo/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<TipoEquipoResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(tipoEquipoService.listar(eq("PC-DESK"), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<TipoEquipoResponse>> respuesta = tipoEquipoController.listar(
                "PC-DESK", null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElTipoEquipo() {

        UUID id = responseEsperado.id();
        when(tipoEquipoService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<TipoEquipoResponse> respuesta = tipoEquipoController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void actualizar_deberiaRetornar200ConElTipoEquipoActualizado() {

        UUID id = responseEsperado.id();
        when(tipoEquipoService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<TipoEquipoResponse> respuesta = tipoEquipoController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = tipoEquipoController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tipoEquipoService).eliminar(id);
    }
}