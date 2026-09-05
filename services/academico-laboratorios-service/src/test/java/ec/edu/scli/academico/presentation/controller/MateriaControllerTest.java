package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.MateriaService;
import ec.edu.scli.academico.presentation.dto.materia.MateriaRequest;
import ec.edu.scli.academico.presentation.dto.materia.MateriaResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MateriaControllerTest {

    @Mock
    private MateriaService materiaService;

    @Mock
    private PoliticaAmbitoAcademico politicaAmbitoAcademico;

    @InjectMocks
    private MateriaController materiaController;

    private UUID carreraId;
    private MateriaRequest requestValido;
    private MateriaResponse responseEsperado;

    @BeforeEach
    void configurar() {
        carreraId = UUID.randomUUID();
        lenient().when(politicaAmbitoAcademico.aplicarCarreraLectura(any(UUID.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        requestValido = new MateriaRequest(carreraId, "PROG1", "Programacion I", 64);
        responseEsperado = new MateriaResponse(
                UUID.randomUUID(), carreraId, "PROG1", "Programacion I", 64,
                true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(materiaService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<MateriaResponse> respuesta = materiaController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/materias/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<MateriaResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(materiaService.listar(eq(carreraId), eq("PROG1"), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<MateriaResponse>> respuesta = materiaController.listar(
                carreraId, "PROG1", null, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConLaMateria() {

        UUID id = responseEsperado.id();
        when(materiaService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<MateriaResponse> respuesta = materiaController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorCarrera_deberiaRetornar200ConLaLista() {

        when(materiaService.listarPorCarrera(carreraId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<MateriaResponse>> respuesta = materiaController.listarPorCarrera(carreraId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void actualizar_deberiaRetornar200ConLaMateriaActualizada() {

        UUID id = responseEsperado.id();
        when(materiaService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<MateriaResponse> respuesta = materiaController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void eliminar_deberiaRetornar204SinCuerpo() {

        UUID id = responseEsperado.id();

        ResponseEntity<Void> respuesta = materiaController.eliminar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(materiaService).eliminar(id);
    }
}
