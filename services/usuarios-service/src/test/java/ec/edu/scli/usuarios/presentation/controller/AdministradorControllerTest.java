package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.application.usecase.AdministradorService;
import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorResponse;
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
class AdministradorControllerTest {

    @Mock
    private AdministradorService administradorService;

    private AdministradorController controller;

    private UUID administradorId;
    private UUID perfilId;
    private AdministradorResponse response;

    @BeforeEach
    void setUp() {
        controller = new AdministradorController(administradorService);

        administradorId = UUID.randomUUID();
        perfilId = UUID.randomUUID();
        response = new AdministradorResponse(
                administradorId, perfilId, "ADM-001", "Coordinador", null, true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void crear_deberiaRetornar201ConUbicacion() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", "Coordinador", null, null
        );

        when(administradorService.crear(request)).thenReturn(response);

        ResponseEntity<AdministradorResponse> resultado = controller.crear(request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultado.getHeaders().getLocation())
                .hasToString("/api/v1/administradores/" + administradorId);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void listar_deberiaRetornar200ConPagina() {
        Page<AdministradorResponse> pagina = new PageImpl<>(List.of(response));

        when(administradorService.listar(PageRequest.of(0, 10))).thenReturn(pagina);

        ResponseEntity<Page<AdministradorResponse>> resultado =
                controller.listar(PageRequest.of(0, 10));

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().getContent()).containsExactly(response);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConAdministrador() {
        when(administradorService.obtenerPorId(administradorId)).thenReturn(response);

        ResponseEntity<AdministradorResponse> resultado =
                controller.obtenerPorId(administradorId);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(response);
    }

    @Test
    void actualizar_deberiaRetornar200ConAdministradorActualizado() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-002", "Director", null, false
        );

        when(administradorService.actualizar(administradorId, request)).thenReturn(response);

        ResponseEntity<AdministradorResponse> resultado =
                controller.actualizar(administradorId, request);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(administradorService).actualizar(administradorId, request);
    }
}
