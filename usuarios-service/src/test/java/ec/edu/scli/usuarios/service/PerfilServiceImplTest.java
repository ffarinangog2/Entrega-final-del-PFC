package ec.edu.scli.usuarios.service;

import ec.edu.scli.usuarios.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.dto.perfil.PerfilExistsResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.AdministradorRepository;
import ec.edu.scli.usuarios.repository.DocenteRepository;
import ec.edu.scli.usuarios.repository.EstudianteRepository;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.repository.TecnicoRepository;
import ec.edu.scli.usuarios.service.impl.PerfilServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de PerfilServiceImpl.
 * <p>
 * Se aíslan los repositorios con Mockito para verificar exclusivamente
 * la lógica de negocio del servicio: validaciones de duplicidad,
 * conversión a DTO y resolución de tipos de perfil asociados.
 */
@ExtendWith(MockitoExtension.class)
class PerfilServiceImplTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private DocenteRepository docenteRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private AdministradorRepository administradorRepository;

    @InjectMocks
    private PerfilServiceImpl perfilService;

    private UUID perfilId;
    private Perfil perfil;

    @BeforeEach
    void setUp() {
        perfilId = UUID.randomUUID();

        perfil = new Perfil();
        perfil.setId(perfilId);
        perfil.setIdentificacion("0102030405");
        perfil.setNombres("Ana");
        perfil.setApellidos("Torres");
        perfil.setEmailInstitucional("ana.torres@uteq.edu.ec");
        perfil.setActivo(true);
    }

    // ---------------------------------------------------------------
    // crear()
    // ---------------------------------------------------------------

    @Test
    void crear_deberiaGuardarPerfilYRetornarResponse_cuandoDatosSonValidos() {
        PerfilCreateRequest request = new PerfilCreateRequest(
                "0102030405",
                "Ana",
                "Torres",
                "Ana.Torres@UTEQ.edu.ec",
                null,
                null,
                null,
                LocalDate.of(2000, 1, 1),
                null
        );

        when(perfilRepository.existsByIdentificacion("0102030405"))
                .thenReturn(false);
        when(perfilRepository.existsByEmailInstitucional("ana.torres@uteq.edu.ec"))
                .thenReturn(false);
        when(perfilRepository.save(any(Perfil.class)))
                .thenReturn(perfil);

        PerfilResponse response = perfilService.crear(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(perfilId);
        assertThat(response.emailInstitucional())
                .isEqualTo("ana.torres@uteq.edu.ec");
        verify(perfilRepository).save(any(Perfil.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoIdentificacionYaExiste() {
        PerfilCreateRequest request = new PerfilCreateRequest(
                "0102030405",
                "Ana",
                "Torres",
                "ana.torres@uteq.edu.ec",
                null, null, null, null, null
        );

        when(perfilRepository.existsByIdentificacion("0102030405"))
                .thenReturn(true);

        assertThatThrownBy(() -> perfilService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("0102030405");

        verify(perfilRepository, never()).save(any(Perfil.class));
    }

    @Test
    void crear_deberiaLanzarConflictException_cuandoEmailYaExiste() {
        PerfilCreateRequest request = new PerfilCreateRequest(
                null,
                "Ana",
                "Torres",
                "ana.torres@uteq.edu.ec",
                null, null, null, null, null
        );

        when(perfilRepository.existsByEmailInstitucional(anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> perfilService.crear(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ana.torres@uteq.edu.ec");

        verify(perfilRepository, never()).save(any(Perfil.class));
    }

    // ---------------------------------------------------------------
    // obtenerPorId()
    // ---------------------------------------------------------------

    @Test
    void obtenerPorId_deberiaRetornarPerfil_cuandoExiste() {
        when(perfilRepository.findById(perfilId))
                .thenReturn(Optional.of(perfil));

        PerfilResponse response = perfilService.obtenerPorId(perfilId);

        assertThat(response.id()).isEqualTo(perfilId);
        assertThat(response.nombres()).isEqualTo("Ana");
    }

    @Test
    void obtenerPorId_deberiaLanzarResourceNotFoundException_cuandoNoExiste() {
        when(perfilRepository.findById(perfilId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.obtenerPorId(perfilId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(perfilId.toString());
    }

    // ---------------------------------------------------------------
    // verificarExistencia()
    // ---------------------------------------------------------------

    @Test
    void verificarExistencia_deberiaRetornarExisteTrueYTipoDocente_cuandoPerfilEsDocente() {
        when(perfilRepository.findById(perfilId))
                .thenReturn(Optional.of(perfil));
        when(docenteRepository.existsByPerfilId(perfilId))
                .thenReturn(true);
        when(estudianteRepository.existsByPerfilId(perfilId))
                .thenReturn(false);
        when(tecnicoRepository.existsByPerfilId(perfilId))
                .thenReturn(false);
        when(administradorRepository.existsByPerfilId(perfilId))
                .thenReturn(false);

        PerfilExistsResponse response =
                perfilService.verificarExistencia(perfilId);

        assertThat(response.existe()).isTrue();
        assertThat(response.activo()).isTrue();
        assertThat(response.tiposPerfil()).containsExactly("DOCENTE");
    }

    @Test
    void verificarExistencia_deberiaRetornarExisteFalse_cuandoPerfilNoExiste() {
        when(perfilRepository.findById(perfilId))
                .thenReturn(Optional.empty());

        PerfilExistsResponse response =
                perfilService.verificarExistencia(perfilId);

        assertThat(response.existe()).isFalse();
        assertThat(response.activo()).isFalse();
        assertThat(response.tiposPerfil()).isEmpty();
    }
}