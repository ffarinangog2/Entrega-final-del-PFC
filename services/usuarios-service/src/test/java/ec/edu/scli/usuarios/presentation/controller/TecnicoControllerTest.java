package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.TecnicoService;
import ec.edu.scli.usuarios.presentation.dto.tecnico.TecnicoRequest;
import ec.edu.scli.usuarios.presentation.dto.tecnico.TecnicoResponse;
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
class TecnicoControllerTest {

    @Mock
    private TecnicoService tecnicoService;

    private TecnicoController controller;

    private UUID tecnicoId;
    private UUID perfilId;
    private TecnicoResponse response;

    @BeforeEach
    void setUp() {
        controller = new TecnicoController(tecnicoService);

        tecnicoId = UUID.randomUUID();
        perfilId = UUID.randomUUID();
        response = new TecnicoResponse(
                tecnicoId, perfilId, "TEC-001", "Redes", true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void crear_deberiaRetornar201ConUbicacion() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-001", "Redes", null);

        when(tecnicoService.crear(request)).thenReturn(response);

        ResponseEntity<TecnicoResponse> resultado = controller.crear(request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultado.getHeaders().getLocation())
                .hasToString("/api/v1/tecnicos/" + tecnicoId);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void listar_deberiaRetornar200ConPagina() {
        Page<TecnicoResponse> pagina = new PageImpl<>(List.of(response));

        when(tecnicoService.listar(PageRequest.of(0, 10))).thenReturn(pagina);

        ResponseEntity<Page<TecnicoResponse>> resultado =
                controller.listar(PageRequest.of(0, 10));

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().getContent()).containsExactly(response);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConTecnico() {
        when(tecnicoService.obtenerPorId(tecnicoId)).thenReturn(response);

        ResponseEntity<TecnicoResponse> resultado = controller.obtenerPorId(tecnicoId);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void actualizar_deberiaRetornar200ConTecnicoActualizado() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-002", "Hardware", false);

        when(tecnicoService.actualizar(tecnicoId, request)).thenReturn(response);

        ResponseEntity<TecnicoResponse> resultado =
                controller.actualizar(tecnicoId, request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tecnicoService).actualizar(tecnicoId, request);
    }
}
