package ec.edu.scli.usuarios.infrastructure.bootstrap;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.ContextoAcademicoEstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.infrastructure.security.HmacIdentificacionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialProfilesBootstrapTest {

    @Test
    void creaPerfilesDeterministasConPersistYLaSegundaEjecucionEsIdempotente() {
        PerfilRepository perfiles = mock(PerfilRepository.class);
        AdministradorRepository administradores = mock(AdministradorRepository.class);
        DocenteRepository docentes = mock(DocenteRepository.class);
        EstudianteRepository estudiantes = mock(EstudianteRepository.class);
        ContextoAcademicoEstudianteRepository contextos = mock(ContextoAcademicoEstudianteRepository.class);
        AdscripcionInstitucionalRepository adscripciones = mock(AdscripcionInstitucionalRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        HmacIdentificacionService hmac = mock(HmacIdentificacionService.class);
        UUID pisoUno = UUID.fromString("35000000-0000-0000-0000-000000000001");
        UUID pisoDos = UUID.fromString("35000000-0000-0000-0000-000000000002");
        UUID pisoTres = UUID.fromString("35000000-0000-0000-0000-000000000003");
        UUID pisoCuatro = UUID.fromString("35000000-0000-0000-0000-000000000004");
        UUID carreraUno = UUID.fromString("37000000-0000-0000-0000-000000000001");
        UUID carreraDos = UUID.fromString("55584c85-0359-3f3f-8fa1-55000611374b");
        InitialProfilesBootstrap bootstrap = new InitialProfilesBootstrap(
                perfiles, administradores, docentes, estudiantes, contextos, adscripciones, entityManager, hmac,
                pisoUno + "," + pisoDos + "," + pisoTres + "," + pisoCuatro, carreraUno + "," + carreraDos,
                UUID.fromString("3f000000-0000-0000-0000-000000000001"));

        when(hmac.calcularHash(any())).thenReturn("hash");
        when(perfiles.getReferenceById(any())).thenAnswer(invocation -> {
            Perfil perfil = new Perfil(); perfil.setId(invocation.getArgument(0)); return perfil;
        });
        when(estudiantes.save(any())).thenAnswer(invocation -> {
            Estudiante estudiante = invocation.getArgument(0);
            if (estudiante.getId() == null) estudiante.setId(UUID.randomUUID());
            return estudiante;
        });
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(any())).thenReturn(List.of());

        bootstrap.run(null);

        var entidadesPersistidas = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(entityManager, org.mockito.Mockito.times(646)).persist(entidadesPersistidas.capture());
        assertEquals(228, entidadesPersistidas.getAllValues().stream().filter(Perfil.class::isInstance).count());
        assertEquals(2, entidadesPersistidas.getAllValues().stream().filter(Administrador.class::isInstance).count());
        assertEquals(18, entidadesPersistidas.getAllValues().stream().filter(Docente.class::isInstance).count());
        assertEquals(198, entidadesPersistidas.getAllValues().stream().filter(Estudiante.class::isInstance).count());
        assertEquals(200, entidadesPersistidas.getAllValues().stream()
                .filter(ContextoAcademicoEstudianteEntity.class::isInstance).count());
        assertTrue(entidadesPersistidas.getAllValues().stream()
                .filter(Administrador.class::isInstance).map(Administrador.class::cast)
                .allMatch(value -> value.getId() != null));
        assertTrue(entidadesPersistidas.getAllValues().stream()
                .filter(Docente.class::isInstance).map(Docente.class::cast)
                .allMatch(value -> value.getId() != null));
        assertTrue(entidadesPersistidas.getAllValues().stream()
                .filter(Estudiante.class::isInstance).map(Estudiante.class::cast)
                .allMatch(value -> value.getId() != null));
        assertTrue(entidadesPersistidas.getAllValues().stream()
                .filter(ContextoAcademicoEstudianteEntity.class::isInstance)
                .map(ContextoAcademicoEstudianteEntity.class::cast)
                .allMatch(value -> value.getId() != null));
        verify(perfiles, never()).save(any());
        verify(administradores, org.mockito.Mockito.times(2)).save(any());
        verify(docentes, org.mockito.Mockito.times(2)).save(any());
        verify(estudiantes, org.mockito.Mockito.times(2)).save(any());
        verify(contextos, never()).save(any());
        verify(adscripciones, org.mockito.Mockito.times(26)).save(any());

        when(perfiles.existsById(any())).thenReturn(true);
        when(administradores.existsByPerfilId(any())).thenReturn(true);
        when(administradores.findByPerfilId(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return java.util.Optional.of(adminParaId(id, pisoUno, pisoDos));
        });
        when(docentes.existsByPerfilId(any())).thenReturn(true);
        when(estudiantes.existsByPerfilId(any())).thenReturn(true);
        when(estudiantes.findByPerfilId(any())).thenAnswer(invocation -> {
            Estudiante estudiante = new Estudiante(); estudiante.setId(UUID.randomUUID());
            Perfil perfil = new Perfil(); perfil.setId(invocation.getArgument(0)); estudiante.setPerfil(perfil);
            return java.util.Optional.of(estudiante);
        });
        when(contextos.findByEstudianteIdAndPeriodoId(any(), any())).thenReturn(java.util.Optional.of(new ec.edu.scli.usuarios.infrastructure.persistence.entity.ContextoAcademicoEstudianteEntity()));
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(any()))
                .thenReturn(List.of(adscripcion(carreraUno), adscripcion(carreraDos)));
        clearInvocations(entityManager, administradores, docentes, estudiantes, contextos, adscripciones);

        bootstrap.run(null);

        verify(entityManager, never()).persist(any());
        verify(docentes, never()).save(any());
        verify(estudiantes, never()).save(any());
        verify(contextos, never()).save(any());
        verify(adscripciones, never()).save(any());
    }

    @Test
    void corrigePisoYActividadDeAdministradoresOperativosSinModificarGlobales() {
        PerfilRepository perfiles = mock(PerfilRepository.class);
        AdministradorRepository administradores = mock(AdministradorRepository.class);
        DocenteRepository docentes = mock(DocenteRepository.class);
        EstudianteRepository estudiantes = mock(EstudianteRepository.class);
        ContextoAcademicoEstudianteRepository contextos = mock(ContextoAcademicoEstudianteRepository.class);
        AdscripcionInstitucionalRepository adscripciones = mock(AdscripcionInstitucionalRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        HmacIdentificacionService hmac = mock(HmacIdentificacionService.class);
        UUID pisoUno = UUID.fromString("35000000-0000-0000-0000-000000000001");
        UUID pisoDos = UUID.fromString("35000000-0000-0000-0000-000000000002");
        UUID pisoTres = UUID.fromString("35000000-0000-0000-0000-000000000003");
        UUID pisoCuatro = UUID.fromString("35000000-0000-0000-0000-000000000004");
        UUID carreraUno = UUID.fromString("37000000-0000-0000-0000-000000000001");
        UUID carreraDos = UUID.fromString("55584c85-0359-3f3f-8fa1-55000611374b");
        InitialProfilesBootstrap bootstrap = new InitialProfilesBootstrap(
                perfiles, administradores, docentes, estudiantes, contextos, adscripciones, entityManager, hmac,
                pisoUno + "," + pisoDos + "," + pisoTres + "," + pisoCuatro, carreraUno + "," + carreraDos,
                UUID.fromString("3f000000-0000-0000-0000-000000000001"));

        when(perfiles.existsById(any())).thenReturn(true);
        when(perfiles.getReferenceById(any())).thenAnswer(invocation -> { Perfil perfil = new Perfil(); perfil.setId(invocation.getArgument(0)); return perfil; });
        when(estudiantes.save(any())).thenAnswer(invocation -> { Estudiante estudiante = invocation.getArgument(0); if (estudiante.getId() == null) estudiante.setId(UUID.randomUUID()); return estudiante; });
        when(docentes.existsByPerfilId(any())).thenReturn(true);
        when(estudiantes.existsByPerfilId(any())).thenReturn(true);
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(any())).thenReturn(List.of());

        Administrador admin1 = adminParaId(perfilId(1), pisoUno, pisoDos);
        Administrador admin2 = adminParaId(perfilId(2), pisoUno, pisoDos);
        Administrador adminCanonico = new Administrador();
        adminCanonico.setCodigoAdministrador("ADM-0001");
        adminCanonico.setPisoId(pisoUno);
        adminCanonico.setActivo(false);
        Administrador admin3 = adminParaId(perfilId(3), pisoUno, pisoDos);
        admin3.setPisoId(pisoTres);
        admin3.setActivo(false);
        Administrador admin4 = adminParaId(perfilId(4), pisoUno, pisoDos);
        admin4.setPisoId(null);
        Administrador adminPisoTres = adminPiso("adminpiso.03", "ADM-PISO-03", pisoUno);
        Administrador adminPisoCuatro = adminPiso("adminpiso.04", "ADM-PISO-04", null);

        when(administradores.findByPerfilId(perfilId(1))).thenReturn(java.util.Optional.of(admin1));
        when(administradores.findByPerfilId(perfilId(2))).thenReturn(java.util.Optional.of(admin2));
        when(administradores.findByCodigoAdministrador("ADM-0001"))
                .thenReturn(java.util.Optional.of(adminCanonico));
        when(administradores.findByPerfilId(perfilId(3))).thenReturn(java.util.Optional.of(admin3));
        when(administradores.findByPerfilId(perfilId(4))).thenReturn(java.util.Optional.of(admin4));
        when(administradores.findByPerfilId(stableId("profile:adminpiso.03")))
                .thenReturn(java.util.Optional.of(adminPisoTres));
        when(administradores.findByPerfilId(stableId("profile:adminpiso.04")))
                .thenReturn(java.util.Optional.of(adminPisoCuatro));

        bootstrap.run(null);

        assertEquals(pisoUno, admin3.getPisoId());
        assertTrue(admin3.getActivo());
        assertEquals(pisoDos, admin4.getPisoId());
        assertEquals(pisoTres, adminPisoTres.getPisoId());
        assertEquals(pisoCuatro, adminPisoCuatro.getPisoId());
        assertTrue(adminPisoTres.getActivo());
        assertTrue(adminPisoCuatro.getActivo());
        assertNull(admin1.getPisoId());
        assertNull(admin2.getPisoId());
        assertTrue(!admin1.getActivo());
        assertTrue(!admin2.getActivo());
        assertNull(adminCanonico.getPisoId());
        assertTrue(adminCanonico.getActivo());
        verify(administradores, never()).save(admin1);
        verify(administradores, never()).save(admin2);
        verify(entityManager, never()).persist(any(Administrador.class));
    }

    private static Administrador adminPiso(String username, String code, UUID pisoId) {
        Administrador admin = new Administrador();
        Perfil perfil = new Perfil();
        perfil.setId(stableId("profile:" + username));
        admin.setPerfil(perfil);
        admin.setCodigoAdministrador(code);
        admin.setCargo("Administración de piso");
        admin.setPisoId(pisoId);
        admin.setActivo(false);
        return admin;
    }

    private static Administrador adminParaId(UUID id, UUID pisoUno, UUID pisoDos) {
        Administrador admin = new Administrador();
        Perfil perfil = new Perfil();
        perfil.setId(id);
        admin.setPerfil(perfil);
        if (id.equals(perfilId(1))) {
            admin.setCodigoAdministrador("ADM-GLOBAL-01");
            admin.setCargo("Administración global");
            admin.setPisoId(null);
        } else if (id.equals(perfilId(2))) {
            admin.setCodigoAdministrador("ADM-GLOBAL-02");
            admin.setCargo("Administración global");
            admin.setPisoId(null);
        } else if (id.equals(perfilId(3))) {
            admin.setCodigoAdministrador("ADM-PISO-01");
            admin.setCargo("Administración de piso");
            admin.setPisoId(pisoUno);
        } else if (id.equals(perfilId(4))) {
            admin.setCodigoAdministrador("ADM-PISO-02");
            admin.setCargo("Administración de piso");
            admin.setPisoId(pisoDos);
        }
        admin.setActivo(true);
        return admin;
    }

    private static AdscripcionInstitucionalEntity adscripcion(UUID carreraId) {
        AdscripcionInstitucionalEntity entity = new AdscripcionInstitucionalEntity();
        entity.setTipoAmbito(TipoAmbitoInstitucional.CARRERA);
        entity.setAmbitoId(carreraId);
        return entity;
    }

    private static UUID perfilId(int number) {
        return UUID.fromString("22000000-0000-0000-0000-0000000000" + String.format("%02d", number));
    }

    private static UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(("scli-integral-test:" + value).getBytes(StandardCharsets.UTF_8));
    }
}
