package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.PisoService;
import ec.edu.scli.academico.presentation.dto.piso.PisoRequest;
import ec.edu.scli.academico.presentation.dto.piso.PisoResponse;
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
class PisoControllerTest {

    @Mock
    private PisoService pisoService;

    @InjectMocks
    private PisoController pisoController;

    private UUID bloqueId;
    private PisoRequest requestValido;
    private PisoResponse responseEsperado;

    @BeforeEach
    void configurar() {
        bloqueId = UUID.randomUUID();
        requestValido = new PisoRequest(bloqueId, 2, "Piso de laboratorios de software");
        responseEsperado = new PisoResponse(
                UUID.randomUUID(), bloqueId, 2, "Piso de laboratorios de software",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(pisoService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<PisoResponse> respuesta = pisoController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/pisos/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<PisoResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(pisoService.listar(eq(bloqueId), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<PisoResponse>> respuesta =
                pisoController.listar(bloqueId, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElPiso() {

        UUID id = responseEsperado.id();
        when(pisoService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<PisoResponse> respuesta = pisoController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorBloque_deberiaRetornar200ConLaLista() {

        when(pisoService.listarPorBloque(bloqueId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<PisoResponse>> respuesta = pisoController.listarPorBloque(bloqueId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void actualizar_deberiaRetornar200ConElPisoActualizado() {

        UUID id = responseEsperado.id();
        when(pisoService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<PisoResponse> respuesta = pisoController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = pisoController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(pisoService).eliminar(id);
    }
}