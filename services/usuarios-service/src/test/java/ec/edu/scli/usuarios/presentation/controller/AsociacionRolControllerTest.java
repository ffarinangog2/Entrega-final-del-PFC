package ec.edu.scli.usuarios.presentation.controller;

import ec.edu.scli.usuarios.domain.model.TipoAmbitoInstitucional;
import ec.edu.scli.usuarios.application.service.AsociacionRolService;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.AdscripcionInstitucionalEntity;
import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdministradorRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.AdscripcionInstitucionalRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.PerfilRepository;
import ec.edu.scli.usuarios.presentation.dto.usuarios.AsociacionRolRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsociacionRolControllerTest {
    @Mock PerfilRepository perfiles;
    @Mock AdministradorRepository administradores;
    @Mock AdscripcionInstitucionalRepository adscripciones;
    private AsociacionRolService service;
    private UUID perfilId;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        service = new AsociacionRolService(perfiles, administradores, adscripciones,
                org.mockito.Mockito.mock(ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository.class));
        perfilId = UUID.randomUUID();
        perfil = new Perfil();
        when(perfiles.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilId)).thenReturn(List.of());
    }

    @Test
    void creaAdministradorDePisoConElPisoSeleccionado() {
        UUID piso2 = UUID.randomUUID();
        when(administradores.findByPerfilId(perfilId)).thenReturn(Optional.empty());

        service.asociar(perfilId, new AsociacionRolRequest("ADMINISTRADOR_PISO", piso2, null));

        ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
        verify(administradores).save(captor.capture());
        assertThat(captor.getValue().getPisoId()).isEqualTo(piso2);
        assertThat(captor.getValue().getActivo()).isTrue();
    }

    @Test
    void creaAdscripcionDeCoordinadorSoloParaLaCarreraSeleccionada() {
        UUID carrera = UUID.randomUUID();
        when(administradores.findByPerfilId(perfilId)).thenReturn(Optional.empty());

        service.asociar(perfilId, new AsociacionRolRequest("COORDINADOR", null, carrera));

        ArgumentCaptor<AdscripcionInstitucionalEntity> captor = ArgumentCaptor.forClass(AdscripcionInstitucionalEntity.class);
        verify(adscripciones).save(captor.capture());
        assertThat(captor.getValue().getTipoAmbito()).isEqualTo(TipoAmbitoInstitucional.CARRERA);
        assertThat(captor.getValue().getAmbitoId()).isEqualTo(carrera);
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    void coordinadoresDistintosConservanCarrerasDistintas() {
        UUID software = UUID.randomUUID();
        UUID tecnologias = UUID.randomUUID();
        UUID perfilB = UUID.randomUUID();
        Perfil segundoPerfil = new Perfil();
        when(perfiles.findById(perfilB)).thenReturn(Optional.of(segundoPerfil));
        when(administradores.findByPerfilId(perfilId)).thenReturn(Optional.empty());
        when(administradores.findByPerfilId(perfilB)).thenReturn(Optional.empty());
        when(adscripciones.findByPerfilIdOrderByTipoAmbitoAscAmbitoIdAsc(perfilB)).thenReturn(List.of());

        service.asociar(perfilId, new AsociacionRolRequest("COORDINADOR", null, software));
        service.asociar(perfilB, new AsociacionRolRequest("COORDINADOR", null, tecnologias));

        ArgumentCaptor<AdscripcionInstitucionalEntity> captor = ArgumentCaptor.forClass(AdscripcionInstitucionalEntity.class);
        verify(adscripciones, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AdscripcionInstitucionalEntity::getAmbitoId)
                .containsExactly(software, tecnologias);
    }
}
