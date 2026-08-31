package ec.edu.scli.academico.presentation.controller;

import ec.edu.scli.academico.application.service.EquipoService;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoEstadoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.presentation.dto.equipo.EquipoResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoControllerTest {

    @Mock
    private EquipoService equipoService;

    @Mock
    private PoliticaAmbitoAcademico politicaAmbitoAcademico;

    @InjectMocks
    private EquipoController equipoController;

    private UUID laboratorioId;
    private EquipoRequest requestValido;
    private EquipoResponse responseEsperado;

    @BeforeEach
    void configurar() {
        laboratorioId = UUID.randomUUID();
        UUID tipoEquipoId = UUID.randomUUID();

        requestValido = new EquipoRequest(
                laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF", "Sin observaciones");

        responseEsperado = new EquipoResponse(
                UUID.randomUUID(), laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF",
                EstadoEquipo.OPERATIVO, "Sin observaciones", true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void crear_deberiaRetornar201ConUbicacionYCuerpo() {

        when(equipoService.crear(requestValido)).thenReturn(responseEsperado);

        ResponseEntity<EquipoResponse> respuesta = equipoController.crear(requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString("/api/v1/equipos/" + responseEsperado.id());
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listar_deberiaRetornar200ConPaginaDeResultados() {

        Page<EquipoResponse> pagina = new PageImpl<>(List.of(responseEsperado));

        when(equipoService.listar(eq(laboratorioId), eq(EstadoEquipo.OPERATIVO), eq(true), any(Pageable.class)))
                .thenReturn(pagina);

        ResponseEntity<Page<EquipoResponse>> respuesta = equipoController.listar(
                laboratorioId, EstadoEquipo.OPERATIVO, true, Pageable.unpaged());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_deberiaRetornar200ConElEquipo() {

        UUID id = responseEsperado.id();
        when(equipoService.obtenerPorId(id)).thenReturn(responseEsperado);

        ResponseEntity<EquipoResponse> respuesta = equipoController.obtenerPorId(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void listarPorLaboratorio_deberiaRetornar200ConLaLista() {

        when(equipoService.listarPorLaboratorio(laboratorioId)).thenReturn(List.of(responseEsperado));

        ResponseEntity<List<EquipoResponse>> respuesta =
                equipoController.listarPorLaboratorio(laboratorioId);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(1);
    }

    @Test
    void actualizar_deberiaRetornar200ConElEquipoActualizado() {

        UUID id = responseEsperado.id();
        when(equipoService.obtenerPorId(id)).thenReturn(responseEsperado);
        when(equipoService.actualizar(id, requestValido)).thenReturn(responseEsperado);

        ResponseEntity<EquipoResponse> respuesta = equipoController.actualizar(id, requestValido);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(responseEsperado);
    }

    @Test
    void cambiarEstado_deberiaRetornar200ConElEstadoActualizado() {

        UUID id = responseEsperado.id();
        EquipoEstadoRequest estadoRequest = new EquipoEstadoRequest(EstadoEquipo.MANTENIMIENTO);

        EquipoResponse responseEnMantenimiento = new EquipoResponse(
                id, laboratorioId, responseEsperado.tipoEquipoId(), "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF",
                EstadoEquipo.MANTENIMIENTO, "Sin observaciones", true,
                OffsetDateTime.now(), OffsetDateTime.now());

        when(equipoService.obtenerPorId(id)).thenReturn(responseEsperado);
        when(equipoService.cambiarEstado(id, EstadoEquipo.MANTENIMIENTO))
                .thenReturn(responseEnMantenimiento);

        ResponseEntity<EquipoResponse> respuesta = equipoController.cambiarEstado(id, estadoRequest);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoEquipo.MANTENIMIENTO);
    }
}
