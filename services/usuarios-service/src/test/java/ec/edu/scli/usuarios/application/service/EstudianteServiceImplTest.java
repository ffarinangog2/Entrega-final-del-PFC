package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.domain.exception.BusinessRuleException;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.model.Estudiante;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.infrastructure.audit.AuditLogger;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteResponse;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceImplTest {

    @Mock
    private EstudianteRepositoryPort estudianteRepository;

    @Mock
    private PerfilRepositoryPort perfilRepository;

    @Mock
    private AuditLogger auditLogger;

    private EstudianteServiceImpl estudianteService;

    private UUID perfilId;
    private UUID estudianteId;
    private Perfil perfil;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        perfilId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();

        perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setActivo(true);

        estudiante = new Estudiante();
        estudiante.setId(estudianteId);
        estudiante.setPerfil(perfil);
        estudiante.setMatricula("MAT-001");
        estudiante.setSemestre(3);
        estudiante.setActivo(true);

        estudianteService = new EstudianteServiceImpl(
                estudianteRepository,
                perfilRepository,
                auditLogger
        );
    }

    // ---------------------------------------------------------------
    // crear()
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaGuardarEstudianteYRetornarResponse_cuandoDatosSonValidos() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(estudianteRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);

        EstudianteResponse response = estudianteService.crear(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(estudianteId);
        assertThat(response.matricula()).isEqualTo("MAT-001");
        verify(estudianteRepository).save(any(Estudiante.class));
    }

    @Test
    void crear_deberiaLanzarResourceNotFoundException_cuandoPerfilNoExiste() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(perfilId.toString());

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    void crear_deberiaLanzarBusinessRuleException_cuandoPerfilInactivo() {
        perfil.setActivo(false);
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> estudianteService.crear(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoPerfilYaEsEstudiante() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.crear(request))
                .isInstanceOf(ConflictException.class);

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoMatriculaYaExiste() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(estudianteRepository.existsByMatricula("MAT-001")).thenReturn(true);

        assertThatThrownBy(() -> estudianteService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("MAT-001");

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    void crear_deberiaMarcarActivoTrue_cuandoActivoNoSeEnviaEnRequest() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(estudianteRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(invocation -> {
            Estudiante guardado = invocation.getArgument(0);
            assertThat(guardado.getActivo()).isTrue();
            guardado.setId(estudianteId);
            return guardado;
        });

        estudianteService.crear(request);

        verify(estudianteRepository).save(any(Estudiante.class));
    }

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    void listar_deberiaRetornarPaginaDeEstudiantes() {
        PageResult<Estudiante> pageResult = new PageResult<>(
                List.of(estudiante), 1, 1, 0, 10
        );

        when(estudianteRepository.findAll(any())).thenReturn(pageResult);

        Page<EstudianteResponse> response =
                estudianteService.listar(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).matricula()).isEqualTo("MAT-001");
    }

    // ---------------------------------------------------------------
    // obtenerPorId()
    // ---------------------------------------------------------------

    @Test
    void obtenerPorId_deberiaRetornarEstudiante_cuandoExiste() {
        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));

        EstudianteResponse response = estudianteService.obtenerPorId(estudianteId);

        assertThat(response.id()).isEqualTo(estudianteId);
        assertThat(response.perfilId()).isEqualTo(perfilId);
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.obtenerPorId(estudianteId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(estudianteId.toString());
    }

    // ---------------------------------------------------------------
    // actualizar()
    // ---------------------------------------------------------------

    @Test
    void actualizar_deberiaActualizarEstudiante_cuandoDatosSonValidos() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-002", null, 5, false
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EstudianteResponse response =
                estudianteService.actualizar(estudianteId, request);

        assertThat(response.matricula()).isEqualTo("MAT-002");
        assertThat(response.semestre()).isEqualTo(5);
        assertThat(response.activo()).isFalse();
    }

    @Test
    void actualizar_deberiaLanzarBusinessRuleException_cuandoCambiaPerfilAsociado() {
        UUID otroPerfilId = UUID.randomUUID();
        EstudianteRequest request = new EstudianteRequest(
                otroPerfilId, "MAT-002", null, 5, null
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> estudianteService.actualizar(estudianteId, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    void actualizar_deberiaLanzarConflictException_cuandoMatriculaYaUsadaPorOtro() {
        Estudiante otro = new Estudiante();
        otro.setId(UUID.randomUUID());
        otro.setPerfil(perfil);
        otro.setMatricula("MAT-002");

        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-002", null, 5, null
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante, otro));

        assertThatThrownBy(() -> estudianteService.actualizar(estudianteId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("MAT-002");

        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    // ---------------------------------------------------------------
    // auditoría
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaAuditarUsuarioCreado_cuandoDatosSonValidos() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, null
        );

        when(perfilRepository.findById(perfilId)).thenReturn(Optional.of(perfil));
        when(estudianteRepository.existsByPerfilId(perfilId)).thenReturn(false);
        when(estudianteRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);

        estudianteService.crear(request);

        verify(auditLogger).registrarEvento(
                eq("usuario_creado"),
                any(),
                any(),
                contains("id=" + estudianteId)
        );
    }

    @Test
    void actualizar_deberiaAuditarUsuarioDesactivado_cuandoActivoCambiaATrueAFalse() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, false
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        estudianteService.actualizar(estudianteId, request);

        verify(auditLogger).registrarEvento(
                eq("usuario_desactivado"),
                any(),
                any(),
                contains("id=" + estudianteId)
        );
    }

    @Test
    void actualizar_deberiaAuditarUsuarioReactivado_cuandoActivoCambiaDeFalseATrue() {
        estudiante.setActivo(false);

        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, true
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        estudianteService.actualizar(estudianteId, request);

        verify(auditLogger).registrarEvento(
                eq("usuario_reactivado"),
                any(),
                any(),
                contains("id=" + estudianteId)
        );
    }

    @Test
    void actualizar_noDeberiaAuditar_cuandoActivoNoCambia() {
        EstudianteRequest request = new EstudianteRequest(
                perfilId, "MAT-001", null, 3, true
        );

        when(estudianteRepository.findById(estudianteId))
                .thenReturn(Optional.of(estudiante));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        estudianteService.actualizar(estudianteId, request);

        verify(auditLogger, never()).registrarEvento(any(), any(), any(), any());
    }
}
