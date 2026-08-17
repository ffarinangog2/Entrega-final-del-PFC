package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.Tecnico;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.domain.port.TecnicoRepositoryPort;
import ec.edu.scli.usuarios.presentation.dto.tecnico.TecnicoRequest;
import ec.edu.scli.usuarios.presentation.dto.tecnico.TecnicoResponse;
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
class TecnicoServiceImplTest {

    @Mock
    private TecnicoRepositoryPort tecnicoRepository;

    @Mock
    private PerfilRepositoryPort perfilRepository;

    private TecnicoServiceImpl tecnicoService;

    private UUID perfilId;
    private UUID tecnicoId;
    private Perfil perfil;
    private Tecnico tecnico;

    @BeforeEach
    void setUp() {
        perfilId = UUID.randomUUID();
        tecnicoId = UUID.randomUUID();

        perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setActivo(true);

        tecnico = new Tecnico();
        tecnico.setId(tecnicoId);
        tecnico.setPerfil(perfil);
        tecnico.setCodigoTecnico("TEC-001");
        tecnico.setActivo(true);

        tecnicoService = new TecnicoServiceImpl(tecnicoRepository, perfilRepository);
    }

    // ---------------------------------------------------------------
    // crear()
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaGuardarTecnicoYRetornarResponse_cuandoDatosSonValidos() {
        TecnicoRequest request = new TecnicoRequest(
                perfilId, "TEC-001", "Redes", null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(tecnicoRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(tecnicoRepository.existsByCodigoTecnico("TEC-001")).thenReturn(false);
        when(tecnicoRepository.save(any(Tecnico.class))).thenReturn(tecnico);

        TecnicoResponse response = tecnicoService.crear(request);

        assertThat(response.id()).isEqualTo(tecnicoId);
        assertThat(response.codigoTecnico()).isEqualTo("TEC-001");
        verify(tecnicoRepository).save(any(Tecnico.class));
    }

    @Test
    void crear_deberiaLanzarResourceNotFoundException_cuandoPerfilNoExiste() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-001", null, null);

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tecnicoService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleException_cuandoPerfilInactivo() {
        perfil.setActivo(false);
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-001", null, null);

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> tecnicoService.crear(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoPerfilYaEsTecnico() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-001", null, null);

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(tecnicoRepository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThatThrownBy(() -> tecnicoService.crear(request))
                .isInstanceOf(ConflictException.class);

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoCodigoYaExiste() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-001", null, null);

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(tecnicoRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(tecnicoRepository.existsByCodigoTecnico("TEC-001")).thenReturn(true);

        assertThatThrownBy(() -> tecnicoService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("TEC-001");

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    void listar_deberiaRetornarPaginaDeTecnicos() {
        PageResult<Tecnico> pageResult = new PageResult<>(List.of(tecnico), 1, 1, 0, 10);

        when(tecnicoRepository.findAll(any())).thenReturn(pageResult);

        Page<TecnicoResponse> response = tecnicoService.listar(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).codigoTecnico()).isEqualTo("TEC-001");
    }

    // ---------------------------------------------------------------
    // obtenerPorId()
    // ---------------------------------------------------------------

    @Test
    void obtenerPorId_deberiaRetornarTecnico_cuandoExiste() {
        when(tecnicoRepository.findById(tecnicoId)).thenReturn(Optional.of(tecnico));

        TecnicoResponse response = tecnicoService.obtenerPorId(tecnicoId);

        assertThat(response.id()).isEqualTo(tecnicoId);
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(tecnicoRepository.findById(tecnicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tecnicoService.obtenerPorId(tecnicoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // actualizar()
    // ---------------------------------------------------------------

    @Test
    void actualizar_deberiaActualizarTecnico_cuandoDatosSonValidos() {
        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-002", "Hardware", false);

        when(tecnicoRepository.findById(tecnicoId)).thenReturn(Optional.of(tecnico));
        when(tecnicoRepository.findAll()).thenReturn(List.of(tecnico));
        when(tecnicoRepository.save(any(Tecnico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TecnicoResponse response = tecnicoService.actualizar(tecnicoId, request);

        assertThat(response.codigoTecnico()).isEqualTo("TEC-002");
        assertThat(response.activo()).isFalse();
    }

    @Test
    void actualizar_deberiaLanzarBusinessRuleException_cuandoCambiaPerfilAsociado() {
        UUID otroPerfilId = UUID.randomUUID();
        TecnicoRequest request = new TecnicoRequest(otroPerfilId, "TEC-002", null, null);

        when(tecnicoRepository.findById(tecnicoId)).thenReturn(Optional.of(tecnico));

        assertThatThrownBy(() -> tecnicoService.actualizar(tecnicoId, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    @Test
    void actualizar_deberiaLanzarConflictException_cuandoCodigoYaUsadoPorOtro() {
        Tecnico otro = new Tecnico();
        otro.setId(UUID.randomUUID());
        otro.setPerfil(perfil);
        otro.setCodigoTecnico("TEC-002");

        TecnicoRequest request = new TecnicoRequest(perfilId, "TEC-002", null, null);

        when(tecnicoRepository.findById(tecnicoId)).thenReturn(Optional.of(tecnico));
        when(tecnicoRepository.findAll()).thenReturn(List.of(tecnico, otro));

        assertThatThrownBy(() -> tecnicoService.actualizar(tecnicoId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("TEC-002");

        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }
}
