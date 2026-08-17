package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorRequest;
import ec.edu.scli.usuarios.presentation.dto.administrador.AdministradorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorServiceImplTest {

    @Mock
    private AdministradorRepositoryPort administradorRepository;

    @Mock
    private PerfilRepositoryPort perfilRepository;

    private AdministradorServiceImpl administradorService;

    private UUID perfilId;
    private UUID administradorId;
    private Perfil perfil;
    private Administrador administrador;

    @BeforeEach
    void setUp() {
        perfilId = UUID.randomUUID();
        administradorId = UUID.randomUUID();

        perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setActivo(true);

        administrador = new Administrador();
        administrador.setId(administradorId);
        administrador.setPerfil(perfil);
        administrador.setCodigoAdministrador("ADM-001");
        administrador.setActivo(true);

        administradorService = new AdministradorServiceImpl(
                administradorRepository, perfilRepository
        );
    }

    // ---------------------------------------------------------------
    // crear()
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaGuardarAdministradorYRetornarResponse_cuandoDatosSonValidos() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", "Coordinador", null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(administradorRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(administradorRepository.existsByCodigoAdministrador("ADM-001")).thenReturn(false);
        when(administradorRepository.save(any(Administrador.class))).thenReturn(administrador);

        AdministradorResponse response = administradorService.crear(request);

        assertThat(response.id()).isEqualTo(administradorId);
        assertThat(response.codigoAdministrador()).isEqualTo("ADM-001");
        verify(administradorRepository).save(any(Administrador.class));
    }

    @Test
    void crear_deberiaLanzarResourceNotFoundException_cuandoPerfilNoExiste() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administradorService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(administradorRepository, never()).save(any(Administrador.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleException_cuandoPerfilInactivo() {
        perfil.setActivo(false);
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> administradorService.crear(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(administradorRepository, never()).save(any(Administrador.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoPerfilYaEsAdministrador() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(administradorRepository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThatThrownBy(() -> administradorService.crear(request))
                .isInstanceOf(ConflictException.class);

        verify(administradorRepository, never()).save(any(Administrador.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoCodigoYaExiste() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-001", null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(administradorRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(administradorRepository.existsByCodigoAdministrador("ADM-001")).thenReturn(true);

        assertThatThrownBy(() -> administradorService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ADM-001");

        verify(administradorRepository, never()).save(any(Administrador.class));
    }

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    void listar_deberiaRetornarPaginaDeAdministradores() {
        PageResult<Administrador> pageResult =
                new PageResult<>(List.of(administrador), 1, 1, 0, 10);

        when(administradorRepository.findAll(any())).thenReturn(pageResult);

        Page<AdministradorResponse> response =
                administradorService.listar(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).codigoAdministrador()).isEqualTo("ADM-001");
    }

    // ---------------------------------------------------------------
    // obtenerPorId()
    // ---------------------------------------------------------------

    @Test
    void obtenerPorId_deberiaRetornarAdministrador_cuandoExiste() {
        when(administradorRepository.findById(administradorId))
                .thenReturn(Optional.of(administrador));

        AdministradorResponse response =
                administradorService.obtenerPorId(administradorId);

        assertThat(response.id()).isEqualTo(administradorId);
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(administradorRepository.findById(administradorId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> administradorService.obtenerPorId(administradorId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // actualizar()
    // ---------------------------------------------------------------

    @Test
    void actualizar_deberiaActualizarAdministrador_cuandoDatosSonValidos() {
        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-002", "Director", null, false
        );

        when(administradorRepository.findById(administradorId))
                .thenReturn(Optional.of(administrador));
        when(administradorRepository.findAll()).thenReturn(List.of(administrador));
        when(administradorRepository.save(any(Administrador.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdministradorResponse response =
                administradorService.actualizar(administradorId, request);

        assertThat(response.codigoAdministrador()).isEqualTo("ADM-002");
        assertThat(response.activo()).isFalse();
    }

    @Test
    void actualizar_deberiaLanzarBusinessRuleException_cuandoCambiaPerfilAsociado() {
        UUID otroPerfilId = UUID.randomUUID();
        AdministradorRequest request = new AdministradorRequest(
                otroPerfilId, "ADM-002", null, null, null
        );

        when(administradorRepository.findById(administradorId))
                .thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> administradorService.actualizar(administradorId, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(administradorRepository, never()).save(any(Administrador.class));
    }

    @Test
    void actualizar_deberiaLanzarConflictException_cuandoCodigoYaUsadoPorOtro() {
        Administrador otro = new Administrador();
        otro.setId(UUID.randomUUID());
        otro.setPerfil(perfil);
        otro.setCodigoAdministrador("ADM-002");

        AdministradorRequest request = new AdministradorRequest(
                perfilId, "ADM-002", null, null, null
        );

        when(administradorRepository.findById(administradorId))
                .thenReturn(Optional.of(administrador));
        when(administradorRepository.findAll()).thenReturn(List.of(administrador, otro));

        assertThatThrownBy(() -> administradorService.actualizar(administradorId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ADM-002");

        verify(administradorRepository, never()).save(any(Administrador.class));
    }
}
