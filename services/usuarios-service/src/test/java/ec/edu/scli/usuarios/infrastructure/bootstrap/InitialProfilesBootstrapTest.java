package ec.edu.scli.usuarios.infrastructure.bootstrap;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
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
