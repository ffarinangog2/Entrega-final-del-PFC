package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.CarreraService;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraRequest;
import ec.edu.scli.academico.presentation.dto.carrera.CarreraResponse;
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
class CarreraControllerTest {

    @Mock
    private CarreraService carreraService;

    @InjectMocks
    private CarreraController carreraController;

    private UUID facultadId;
    private CarreraRequest requestValido;
    private CarreraResponse responseEsperado;

    @BeforeEach
    void configurar() {
        facultadId = UUID.randomUUID();
        requestValido = new CarreraRequest(facultadId, "SOFT", "Ingenieria de Software", "Descripcion");
        responseEsperado = new CarreraResponse(
                UUID.randomUUID(), facultadId, "SOFT", "Ingenieria de Software", "Descripcion",
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(carreraService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<CarreraResponse> respuesta = carreraController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/carreras/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<CarreraResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(carreraService.listar(eq(facultadId), eq("SOFT"), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<CarreraResponse>> respuesta = carreraController.listar(
                facultadId, "SOFT", null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConLaCarrera() {

        UUID id = responseEsperado.id();
        when(carreraService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<CarreraResponse> respuesta = carreraController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorFacultad_deberiaRetornar200ConLaLista() {

        when(carreraService.listarPorFacultad(facultadId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<CarreraResponse>> respuesta = carreraController.listarPorFacultad(facultadId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void actualizar_deberiaRetornar200ConLaCarreraActualizada() {

        UUID id = responseEsperado.id();
        when(carreraService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<CarreraResponse> respuesta = carreraController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = carreraController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(carreraService).eliminar(id);
    }
}