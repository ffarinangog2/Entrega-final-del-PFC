package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.EstudianteService;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteResponse;
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
class EstudianteControllerTest {

    @Mock
    private EstudianteService estudianteService;

    private EstudianteController controller;

    private UUID estudianteId;
    private EstudianteResponse response;

    @BeforeEach
    void setUp() {
        controller = new EstudianteController(estudianteService);

        estudianteId = UUID.randomUUID();
        response = new EstudianteResponse(
                estudianteId,
                UUID.randomUUID(),
                "MAT-001",
                null,
                3,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    void crear_deberiaRetornar201ConUbicacion() {
        EstudianteRequest request = new EstudianteRequest(
                response.perfilId(), "MAT-001", null, 3, null
        );

        when(estudianteService.crear(request)).thenReturn(response);

        ResponseEntity<EstudianteResponse> resultado = controller.crear(request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultado.getHeaders().getLocation())
                .hasToString("/api/v1/estudiantes/" + estudianteId);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void listar_deberiaRetornar200ConPagina() {
        Page<EstudianteResponse> pagina = new PageImpl<>(List.of(response));

        when(estudianteService.listar(PageRequest.of(0, 10))).thenReturn(pagina);

        ResponseEntity<Page<EstudianteResponse>> resultado =
                controller.listar(PageRequest.of(0, 10));

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().getContent()).containsExactly(response);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConEstudiante() {
        when(estudianteService.obtenerPorId(estudianteId)).thenReturn(response);

        ResponseEntity<EstudianteResponse> resultado =
                controller.obtenerPorId(estudianteId);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void actualizar_deberiaRetornar200ConEstudianteActualizado() {
        EstudianteRequest request = new EstudianteRequest(
                response.perfilId(), "MAT-002", null, 5, false
        );

        when(estudianteService.actualizar(estudianteId, request)).thenReturn(response);

        ResponseEntity<EstudianteResponse> resultado =
                controller.actualizar(estudianteId, request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(estudianteService).actualizar(estudianteId, request);
    }
}
