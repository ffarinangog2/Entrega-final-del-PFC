package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Estudiante;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.EstudianteRepository;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilPersistenceMapper;
import ec.edu.scli.usuarios.infrastructure.persistence.mapper.PerfilRelacionadoPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteRepositoryAdapterTest {

    @Mock
    private EstudianteRepository repository;

    @Mock
    private EntityManager entityManager;

    private EstudianteRepositoryAdapter adapter;

    private UUID perfilId;
    private UUID estudianteId;

    @BeforeEach
    void setUp() {
        PerfilRelacionadoPersistenceMapper mapper =
                new PerfilRelacionadoPersistenceMapper(new PerfilPersistenceMapper());

        adapter = new EstudianteRepositoryAdapter(repository, mapper);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);

        perfilId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();
    }

    private ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entidad() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil perfilEntity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();
        perfilEntity.setId(perfilId);
        perfilEntity.setActivo(true);

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante();
        entity.setId(estudianteId);
        entity.setPerfil(perfilEntity);
        entity.setMatricula("MAT-001");
        entity.setSemestre(3);
        entity.setActivo(true);
        return entity;
    }

    private Estudiante dominio() {
        Perfil perfil = new Perfil();
        perfil.setId(perfilId);

        Estudiante estudiante = new Estudiante();
        estudiante.setId(estudianteId);
        estudiante.setPerfil(perfil);
        estudiante.setMatricula("MAT-001");
        estudiante.setSemestre(3);
        estudiante.setActivo(true);
        return estudiante;
    }

    @Test
    void save_deberiaMapearYPersistirUsandoReferenciaDePerfil() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil perfilReferencia =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();
        perfilReferencia.setId(perfilId);

        when(entityManager.getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                perfilId
        )).thenReturn(perfilReferencia);
        when(repository.save(any())).thenReturn(entidad());

        Estudiante guardado = adapter.save(dominio());

        assertThat(guardado.getId()).isEqualTo(estudianteId);
        assertThat(guardado.getMatricula()).isEqualTo("MAT-001");
        verify(entityManager).getReference(
                ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil.class,
                perfilId
        );
    }

    @Test
    void findAll_conPaginacion_deberiaMapearPageResult() {
        Page<ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante> pagina =
                new PageImpl<>(List.of(entidad()), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        PageResult<Estudiante> resultado =
                adapter.findAll(new PageCriteria(0, 10, List.of()));

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).getMatricula()).isEqualTo("MAT-001");
    }

    @Test
    void findAll_sinArgumentos_deberiaMapearListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(entidad()));

        List<Estudiante> resultado = adapter.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(estudianteId);
    }

    @Test
    void findById_deberiaRetornarEstudiante_cuandoExiste() {
        when(repository.findById(estudianteId)).thenReturn(Optional.of(entidad()));

        Optional<Estudiante> resultado = adapter.findById(estudianteId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(estudianteId);
    }

    @Test
    void findByPerfilId_deberiaRetornarEstudiante_cuandoExiste() {
        when(repository.findByPerfilId(perfilId)).thenReturn(Optional.of(entidad()));

        Optional<Estudiante> resultado = adapter.findByPerfilId(perfilId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getPerfil().getId()).isEqualTo(perfilId);
    }

    @Test
    void existsByPerfilId_deberiaDelegarEnRepository() {
        when(repository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThat(adapter.existsByPerfilId(perfilId)).isTrue();
    }

    @Test
    void existsByMatricula_deberiaDelegarEnRepository() {
        when(repository.existsByMatricula("MAT-001")).thenReturn(true);

        assertThat(adapter.existsByMatricula("MAT-001")).isTrue();
    }
}
