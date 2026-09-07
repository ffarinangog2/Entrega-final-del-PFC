package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
import ec.edu.scli.usuarios.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocentePlanificacionControllerTest {

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private AdscripcionInstitucionalRepository adscripcionRepository;

    @Mock
    private Authentication authentication;

    private DocentePlanificacionController controller;

    private UUID coordinadorPerfilId;
    private UUID carreraId;

    @BeforeEach
    void setUp() {
        controller = new DocentePlanificacionController(docenteRepository, adscripcionRepository);
        coordinadorPerfilId = UUID.randomUUID();
        carreraId = UUID.randomUUID();
    }

    private Docente crearDocenteEntity(UUID id, String codigo, String nombres, String apellidos, boolean activo) {
        Perfil perfil = new Perfil();
        perfil.setId(UUID.randomUUID());
        perfil.setNombres(nombres);
        perfil.setApellidos(apellidos);

        Docente docente = new Docente();
        docente.setId(id);
        docente.setPerfil(perfil);
        docente.setCodigoDocente(codigo);
        docente.setActivo(activo);
        docente.setTituloAcademico("Magister");
        docente.setDepartamento("Sistemas");
        docente.setTipoContrato("Tiempo completo");
        docente.setDedicacion("40h");
        return docente;
    }

    private AdscripcionInstitucionalEntity crearAdscripcion(UUID perfilId, UUID ambitoId, TipoAmbitoInstitucional tipo, boolean activo) {
        Perfil p = new Perfil();
        p.setId(perfilId);
        AdscripcionInstitucionalEntity entity = new AdscripcionInstitucionalEntity();
        entity.setPerfil(p);
        entity.setAmbitoId(ambitoId);
        entity.setTipoAmbito(tipo);
        entity.setActivo(activo);
        return entity;
    }

    @Test
    void listar_deberiaRetornarTodosLosDocentesActivosConNombresYApellidosSinFiltrarPorCarrera() {
        when(authentication.getPrincipal()).thenReturn(
                new JwtPrincipal(UUID.randomUUID(), coordinadorPerfilId, "coordinador.sistemas"));

        AdscripcionInstitucionalEntity adscripcion = crearAdscripcion(
                coordinadorPerfilId, carreraId, TipoAmbitoInstitucional.CARRERA, true);
        when(adscripcionRepository.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(coordinadorPerfilId))
                .thenReturn(List.of(adscripcion));

        Docente d1 = crearDocenteEntity(UUID.randomUUID(), "DOC-001", "Ana", "Torres", true);
        Docente d2 = crearDocenteEntity(UUID.randomUUID(), "DOC-002", "Bernardo", "Mendoza", true);
        when(docenteRepository.findTodosActivosConPerfil()).thenReturn(List.of(d1, d2));

        ResponseEntity<List<DocenteResponse>> response = controller.listar(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(2);

        DocenteResponse r1 = response.getBody().get(0);
        assertThat(r1.id()).isEqualTo(d1.getId());
        assertThat(r1.codigoDocente()).isEqualTo("DOC-001");
        assertThat(r1.nombres()).isEqualTo("Ana");
        assertThat(r1.apellidos()).isEqualTo("Torres");
        assertThat(r1.activo()).isTrue();

        DocenteResponse r2 = response.getBody().get(1);
        assertThat(r2.id()).isEqualTo(d2.getId());
        assertThat(r2.codigoDocente()).isEqualTo("DOC-002");
        assertThat(r2.nombres()).isEqualTo("Bernardo");
        assertThat(r2.apellidos()).isEqualTo("Mendoza");
        assertThat(r2.activo()).isTrue();

        // Confirma que se consulta la lista general activa y no se filtra por carrera
        verify(docenteRepository).findTodosActivosConPerfil();
    }

    @Test
    void listar_lanzaAccessDeniedSiCoordinadorNoTieneCarreraUnica() {
        when(authentication.getPrincipal()).thenReturn(
                new JwtPrincipal(UUID.randomUUID(), coordinadorPerfilId, "coordinador.sin.carrera"));
        when(adscripcionRepository.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(coordinadorPerfilId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> controller.listar(authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("carrera institucional");
    }

    @Test
    void listar_lanzaAccessDeniedSiNoHayPrincipalAutenticado() {
        assertThatThrownBy(() -> controller.listar(null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("identidad institucional");
    }
}
