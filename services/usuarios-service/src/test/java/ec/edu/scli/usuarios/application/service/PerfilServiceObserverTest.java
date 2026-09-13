package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.event.PerfilEvent;
import ec.edu.scli.usuarios.domain.event.PerfilEventListener;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilCreateRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilServiceObserverTest {

    @Test
    void crear_debeNotificarEventoPerfilCreado() {
        PerfilRepositoryPort perfilRepository = mock(PerfilRepositoryPort.class);
        List<PerfilEvent> eventos = new ArrayList<>();
        PerfilEventListener listener = eventos::add;

        when(perfilRepository.existsByIdentificacion("OBS-001")).thenReturn(false);
        when(perfilRepository.existsByEmailInstitucional("observer@uteq.edu.ec")).thenReturn(false);
        when(perfilRepository.save(any(Perfil.class))).thenAnswer(invocation -> {
            Perfil perfil = invocation.getArgument(0);
            perfil.setId(UUID.randomUUID());
            return perfil;
        });

        PerfilServiceImpl service = new PerfilServiceImpl(
                perfilRepository,
                mock(DocenteRepositoryPort.class),
                mock(EstudianteRepositoryPort.class),
                mock(AdministradorRepositoryPort.class),
                List.of(listener)
        );

        service.crear(new PerfilCreateRequest(
                "OBS-001",
                "Observer",
                "Test",
                "observer@uteq.edu.ec",
                null,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                null
        ));

        assertThat(eventos).hasSize(1);
        assertThat(eventos.getFirst().tipo()).isEqualTo("PERFIL_CREADO");
        assertThat(eventos.getFirst().activo()).isTrue();
    }
}
