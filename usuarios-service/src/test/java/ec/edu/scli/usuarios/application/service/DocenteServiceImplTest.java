package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.model.Docente;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteRequest;
import ec.edu.scli.usuarios.presentation.dto.docente.DocenteResponse;
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
class DocenteServiceImplTest {

    @Mock
    private DocenteRepositoryPort docenteRepository;

    @Mock
    private PerfilRepositoryPort perfilRepository;

    private DocenteServiceImpl docenteService;

    private UUID perfilId;
    private UUID docenteId;
    private Perfil perfil;
    private Docente docente;

    @BeforeEach
    void setUp() {
        perfilId = UUID.randomUUID();
        docenteId = UUID.randomUUID();

        perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setActivo(true);

        docente = new Docente();
        docente.setId(docenteId);
        docente.setPerfil(perfil);
        docente.setCodigoDocente("DOC-001");
        docente.setActivo(true);

        docenteService = new DocenteServiceImpl(docenteRepository, perfilRepository);
    }

    // ---------------------------------------------------------------
    // crear()
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaGuardarDocenteYRetornarResponse_cuandoDatosSonValidos() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", "Magister", "Sistemas", "Tiempo completo", "40h", null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(docenteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(docenteRepository.existsByCodigoDocente("DOC-001")).thenReturn(false);
        when(docenteRepository.save(any(Docente.class))).thenReturn(docente);

        DocenteResponse response = docenteService.crear(request);

        assertThat(response.id()).isEqualTo(docenteId);
        assertThat(response.codigoDocente()).isEqualTo("DOC-001");
        verify(docenteRepository).save(any(Docente.class));
    }

    @Test
    void crear_deberiaLanzarResourceNotFoundException_cuandoPerfilNoExiste() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", null, null, null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleException_cuandoPerfilInactivo() {
        perfil.setActivo(false);
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", null, null, null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> docenteService.crear(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoPerfilYaEsDocente() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", null, null, null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(docenteRepository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThatThrownBy(() -> docenteService.crear(request))
                .isInstanceOf(ConflictException.class);

        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoCodigoYaExiste() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-001", null, null, null, null, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(docenteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(docenteRepository.existsByCodigoDocente("DOC-001")).thenReturn(true);

        assertThatThrownBy(() -> docenteService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DOC-001");

        verify(docenteRepository, never()).save(any(Docente.class));
    }

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    void listar_deberiaRetornarPaginaDeDocentes() {
        PageResult<Docente> pageResult = new PageResult<>(List.of(docente), 1, 1, 0, 10);

        when(docenteRepository.findAll(any())).thenReturn(pageResult);

        Page<DocenteResponse> response = docenteService.listar(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).codigoDocente()).isEqualTo("DOC-001");
    }

    // ---------------------------------------------------------------
    // obtenerPorId() / obtenerPorPerfilId()
    // ---------------------------------------------------------------

    @Test
    void obtenerPorId_deberiaRetornarDocente_cuandoExiste() {
        when(docenteRepository.findById(docenteId)).thenReturn(Optional.of(docente));

        DocenteResponse response = docenteService.obtenerPorId(docenteId);

        assertThat(response.id()).isEqualTo(docenteId);
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(docenteRepository.findById(docenteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.obtenerPorId(docenteId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorPerfilId_deberiaRetornarDocente_cuandoExiste() {
        when(docenteRepository.findByPerfilId(perfilId)).thenReturn(Optional.of(docente));

        DocenteResponse response = docenteService.obtenerPorPerfilId(perfilId);

        assertThat(response.perfilId()).isEqualTo(perfilId);
    }

    @Test
    void obtenerPorPerfilId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(docenteRepository.findByPerfilId(perfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> docenteService.obtenerPorPerfilId(perfilId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // actualizar()
    // ---------------------------------------------------------------

    @Test
    void actualizar_deberiaActualizarDocente_cuandoDatosSonValidos() {
        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-002", "PhD", "Software", "Medio tiempo", "20h", false
        );

        when(docenteRepository.findById(docenteId)).thenReturn(Optional.of(docente));
        when(docenteRepository.findAll()).thenReturn(List.of(docente));
        when(docenteRepository.save(any(Docente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocenteResponse response = docenteService.actualizar(docenteId, request);

        assertThat(response.codigoDocente()).isEqualTo("DOC-002");
        assertThat(response.activo()).isFalse();
    }

    @Test
    void actualizar_deberiaLanzarBusinessRuleException_cuandoCambiaPerfilAsociado() {
        UUID otroPerfilId = UUID.randomUUID();
        DocenteRequest request = new DocenteRequest(
                otroPerfilId, "DOC-002", null, null, null, null, null
        );

        when(docenteRepository.findById(docenteId)).thenReturn(Optional.of(docente));

        assertThatThrownBy(() -> docenteService.actualizar(docenteId, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    void actualizar_deberiaLanzarConflictException_cuandoCodigoYaUsadoPorOtro() {
        Docente otro = new Docente();
        otro.setId(UUID.randomUUID());
        otro.setPerfil(perfil);
        otro.setCodigoDocente("DOC-002");

        DocenteRequest request = new DocenteRequest(
                perfilId, "DOC-002", null, null, null, null, null
        );

        when(docenteRepository.findById(docenteId)).thenReturn(Optional.of(docente));
        when(docenteRepository.findAll()).thenReturn(List.of(docente, otro));

        assertThatThrownBy(() -> docenteService.actualizar(docenteId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DOC-002");

        verify(docenteRepository, never()).save(any(Docente.class));
    }
}
