package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.model.AdscripcionInstitucional;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.AdscripcionInstitucionalRepositoryPort;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.domain.port.TecnicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextoInstitucionalServiceImplTest {
    @Mock PerfilRepositoryPort perfiles;
    @Mock DocenteRepositoryPort docentes;
    @Mock EstudianteRepositoryPort estudiantes;
    @Mock TecnicoRepositoryPort tecnicos;
    @Mock AdministradorRepositoryPort administradores;
    @Mock AdscripcionInstitucionalRepositoryPort adscripciones;

    private ContextoInstitucionalServiceImpl service;
    private UUID perfilId;

    @BeforeEach
    void setUp() {
        service = new ContextoInstitucionalServiceImpl(
                perfiles, docentes, estudiantes, tecnicos, administradores, adscripciones);
        perfilId = UUID.randomUUID();
    }

    @Test
    void administradorActivoConPisoEsOperativo() {
        Perfil perfil = perfil(true);
        Administrador administrador = administrador(perfil, true, UUID.randomUUID());
        prepararPerfil(perfil, List.of(), Optional.of(administrador));

        var contexto = service.obtenerPorPerfilId(perfilId);

        assertThat(contexto.administrador().esAdministrador()).isTrue();
        assertThat(contexto.administrador().pisoId()).isEqualTo(administrador.getPisoId());
        assertThat(contexto.administrador().administradorPisoOperativo()).isTrue();
    }

    @Test
    void administradorSinPisoNoEsOperativo() {
        Perfil perfil = perfil(true);
        prepararPerfil(perfil, List.of(), Optional.of(administrador(perfil, true, null)));

        assertThat(service.obtenerPorPerfilId(perfilId)
                .administrador().administradorPisoOperativo()).isFalse();
    }

    @Test
    void devuelveAdscripcionCarrera() {
        UUID carreraId = UUID.randomUUID();
        Perfil perfil = perfil(true);
        prepararPerfil(perfil, List.of(adscripcion(TipoAmbitoInstitucional.CARRERA, carreraId)),
                Optional.empty());

        assertThat(service.obtenerPorPerfilId(perfilId).adscripciones())
                .extracting(AdscripcionInstitucional::ambitoId).containsExactly(carreraId);
    }

    @Test
    void devuelveAdscripcionFacultad() {
        UUID facultadId = UUID.randomUUID();
        Perfil perfil = perfil(true);
        prepararPerfil(perfil, List.of(adscripcion(TipoAmbitoInstitucional.FACULTAD, facultadId)),
                Optional.empty());

        assertThat(service.obtenerPorPerfilId(perfilId).adscripciones())
                .extracting(AdscripcionInstitucional::ambitoId).containsExactly(facultadId);
    }

    private void prepararPerfil(
            Perfil perfil,
            List<AdscripcionInstitucional> items,
            Optional<Administrador> administrador) {
        when(perfiles.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(administradores.findByPerfilId(perfilId)).thenReturn(administrador);
        when(adscripciones.findByPerfilId(perfilId)).thenReturn(items);
    }

    private Perfil perfil(boolean activo) {
        Perfil perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setActivo(activo);
        return perfil;
    }

    private Administrador administrador(Perfil perfil, boolean activo, UUID pisoId) {
        Administrador administrador = new Administrador();
        administrador.setPerfil(perfil);
        administrador.setActivo(activo);
        administrador.setPisoId(pisoId);
        administrador.setCargo("Responsable operativo");
        return administrador;
    }

    private AdscripcionInstitucional adscripcion(TipoAmbitoInstitucional tipo, UUID ambitoId) {
        return new AdscripcionInstitucional(
                UUID.randomUUID(), perfilId, tipo, ambitoId, true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }
}
