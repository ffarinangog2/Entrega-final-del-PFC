package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.DocenteService;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteRequest;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocenteControllerTest {

    @Mock
    private DocenteService docenteService;

    private DocenteController controller;

    private UUID docenteId;
    private UUID perfilId;
    private DocenteResponse response;

    @BeforeEach
    void setUp() {
        controller = new DocenteController(docenteService);

        docenteId = UUID.randomUUID();
        perfilId = UUID.randomUUID();
        response = new DocenteResponse(
                docenteId, perfilId, "DOC-001", "Magister", "Sistemas",
                "Tiempo completo", "40h", true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void crear_deberiaRetornar201ConUbicacion() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", "Magister", "Sistemas", "Tiempo completo", "40h", null
        );

        when(docenteService.crear(request)).thenReturn(response);

        ResponseEntity<DocenteResponse> resultado = controller.crear(request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultado.getHeaders().getLocation())
                .hasToString("/api/v1/docentes/" + docenteId);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void listar_deberiaRetornar200ConPagina() {
        Page<DocenteResponse> pagina = new PageImpl<>(List.of(response));

        when(docenteService.listar(PageRequest.of(0, 10))).thenReturn(pagina);

        ResponseEntity<Page<DocenteResponse>> resultado =
                controller.listar(PageRequest.of(0, 10));

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().getContent()).containsExactly(response);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConDocente() {
        when(docenteService.obtenerPorId(docenteId)).thenReturn(response);

        ResponseEntity<DocenteResponse> resultado = controller.obtenerPorId(docenteId);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void actualizar_deberiaRetornar200ConDocenteActualizado() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-002", null, null, null, null, false
        );

        when(docenteService.actualizar(docenteId, request)).thenReturn(response);

        ResponseEntity<DocenteResponse> resultado =
                controller.actualizar(docenteId, request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(docenteService).actualizar(docenteId, request);
    }

    @Test
    void obtenerPorPerfilId_deberiaRetornar200ConDocente() {
        when(docenteService.obtenerPorPerfilId(perfilId)).thenReturn(response);

        ResponseEntity<DocenteResponse> resultado =
                controller.obtenerPorPerfilId(perfilId);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(response);
    }
}
