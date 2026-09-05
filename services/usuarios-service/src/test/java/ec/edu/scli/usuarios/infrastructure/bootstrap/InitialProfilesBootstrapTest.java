package ec.edu.scli.usuarios.infrastructure.bootstrap;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.infrastructure.security.HmacIdentificacionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        AdscripcionInstitucionalRepository adscripciones = mock(AdscripcionInstitucionalRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        HmacIdentificacionService hmac = mock(HmacIdentificacionService.class);
        UUID pisoUno = UUID.fromString("35000000-0000-0000-0000-000000000001");
        UUID pisoDos = UUID.fromString("35000000-0000-0000-0000-000000000002");
        UUID carreraUno = UUID.fromString("37000000-0000-0000-0000-000000000001");
        UUID carreraDos = UUID.fromString("37000000-0000-0000-0000-000000000002");
        InitialProfilesBootstrap bootstrap = new InitialProfilesBootstrap(
                perfiles, administradores, docentes, estudiantes, adscripciones, entityManager, hmac,
                pisoUno + "," + pisoDos, carreraUno + "," + carreraDos);

        when(hmac.calcularHash(any())).thenReturn("hash");
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(any())).thenReturn(List.of());

        bootstrap.run(null);

        var perfilesPersistidos = org.mockito.ArgumentCaptor.forClass(Perfil.class);
        verify(entityManager, org.mockito.Mockito.times(10)).persist(perfilesPersistidos.capture());
        assertEquals(
                java.util.stream.IntStream.rangeClosed(1, 10)
                        .mapToObj(InitialProfilesBootstrapTest::perfilId)
                        .toList(),
                perfilesPersistidos.getAllValues().stream().map(Perfil::getId).toList());
        verify(perfiles, never()).save(any());

        when(perfiles.existsById(any())).thenReturn(true);
        when(administradores.existsByPerfilId(any())).thenReturn(true);
        when(administradores.findByPerfilId(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return java.util.Optional.of(adminParaId(id, pisoUno, pisoDos));
        });
        when(docentes.existsByPerfilId(any())).thenReturn(true);
        when(estudiantes.existsByPerfilId(any())).thenReturn(true);
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId(5)))
                .thenReturn(List.of(adscripcion(carreraUno)));
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId(6)))
                .thenReturn(List.of(adscripcion(carreraDos)));
        clearInvocations(entityManager, administradores, docentes, estudiantes, adscripciones);

        bootstrap.run(null);

        verify(entityManager, never()).persist(any());
        verify(administradores, never()).save(any());
        verify(docentes, never()).save(any());
        verify(estudiantes, never()).save(any());
        verify(adscripciones, never()).save(any());
    }

    @Test
    void actualizaAdministradorSiPisoODatosCambian() {
        PerfilRepository perfiles = mock(PerfilRepository.class);
        AdministradorRepository administradores = mock(AdministradorRepository.class);
        DocenteRepository docentes = mock(DocenteRepository.class);
        EstudianteRepository estudiantes = mock(EstudianteRepository.class);
        AdscripcionInstitucionalRepository adscripciones = mock(AdscripcionInstitucionalRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        HmacIdentificacionService hmac = mock(HmacIdentificacionService.class);
        UUID pisoUno = UUID.fromString("35000000-0000-0000-0000-000000000001");
        UUID pisoDos = UUID.fromString("35000000-0000-0000-0000-000000000002");
        UUID carreraUno = UUID.fromString("37000000-0000-0000-0000-000000000001");
        UUID carreraDos = UUID.fromString("37000000-0000-0000-0000-000000000002");
        InitialProfilesBootstrap bootstrap = new InitialProfilesBootstrap(
                perfiles, administradores, docentes, estudiantes, adscripciones, entityManager, hmac,
                pisoUno + "," + pisoDos, carreraUno + "," + carreraDos);

        when(perfiles.existsById(any())).thenReturn(true);
        when(docentes.existsByPerfilId(any())).thenReturn(true);
        when(estudiantes.existsByPerfilId(any())).thenReturn(true);
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(any())).thenReturn(List.of());

        Administrador admin1 = adminParaId(perfilId(1), pisoUno, pisoDos);
        Administrador admin2 = adminParaId(perfilId(2), pisoUno, pisoDos);
        Administrador admin3 = adminParaId(perfilId(3), pisoUno, pisoDos);
        admin3.setPisoId(null);
        Administrador admin4 = adminParaId(perfilId(4), pisoUno, pisoDos);

        when(administradores.findByPerfilId(perfilId(1))).thenReturn(java.util.Optional.of(admin1));
        when(administradores.findByPerfilId(perfilId(2))).thenReturn(java.util.Optional.of(admin2));
        when(administradores.findByPerfilId(perfilId(3))).thenReturn(java.util.Optional.of(admin3));
        when(administradores.findByPerfilId(perfilId(4))).thenReturn(java.util.Optional.of(admin4));

        bootstrap.run(null);

        assertEquals(pisoUno, admin3.getPisoId());
        verify(administradores).save(admin3);
        verify(administradores, never()).save(admin1);
        verify(administradores, never()).save(admin2);
        verify(administradores, never()).save(admin4);
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
}
