package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Docente;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.DocenteRepository;
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
class DocenteRepositoryAdapterTest {

    @Mock
    private DocenteRepository repository;

    @Mock
    private EntityManager entityManager;

    private DocenteRepositoryAdapter adapter;

    private UUID perfilId;
    private UUID docenteId;

    @BeforeEach
    void setUp() {
        PerfilRelacionadoPersistenceMapper mapper =
                new PerfilRelacionadoPersistenceMapper(new PerfilPersistenceMapper());

        adapter = new DocenteRepositoryAdapter(repository, mapper);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);

        perfilId = UUID.randomUUID();
        docenteId = UUID.randomUUID();
    }

    private ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entidad() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil perfilEntity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();
        perfilEntity.setId(perfilId);
        perfilEntity.setActivo(true);

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente();
        entity.setId(docenteId);
        entity.setPerfil(perfilEntity);
        entity.setCodigoDocente("DOC-001");
        entity.setActivo(true);
        return entity;
    }

    private Docente dominio() {
        Perfil perfil = new Perfil();
        perfil.setId(perfilId);

        Docente docente = new Docente();
        docente.setId(docenteId);
        docente.setPerfil(perfil);
        docente.setCodigoDocente("DOC-001");
        docente.setActivo(true);
        return docente;
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

        Docente guardado = adapter.save(dominio());

        assertThat(guardado.getId()).isEqualTo(docenteId);
        assertThat(guardado.getCodigoDocente()).isEqualTo("DOC-001");
    }

    @Test
    void findAll_conPaginacion_deberiaMapearPageResult() {
        Page<ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente> pagina =
                new PageImpl<>(List.of(entidad()), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        PageResult<Docente> resultado =
                adapter.findAll(new PageCriteria(0, 10, List.of()));

        assertThat(resultado.content()).hasSize(1);
    }

    @Test
    void findAll_sinArgumentos_deberiaMapearListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(entidad()));

        List<Docente> resultado = adapter.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(docenteId);
    }

    @Test
    void findById_deberiaRetornarDocente_cuandoExiste() {
        when(repository.findById(docenteId)).thenReturn(Optional.of(entidad()));

        Optional<Docente> resultado = adapter.findById(docenteId);

        assertThat(resultado).isPresent();
    }

    @Test
    void findByPerfilId_deberiaRetornarDocente_cuandoExiste() {
        when(repository.findByPerfilId(perfilId)).thenReturn(Optional.of(entidad()));

        Optional<Docente> resultado = adapter.findByPerfilId(perfilId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getPerfil().getId()).isEqualTo(perfilId);
    }

    @Test
    void existsByPerfilId_deberiaDelegarEnRepository() {
        when(repository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThat(adapter.existsByPerfilId(perfilId)).isTrue();
    }

    @Test
    void existsByCodigoDocente_deberiaDelegarEnRepository() {
        when(repository.existsByCodigoDocente("DOC-001")).thenReturn(true);

        assertThat(adapter.existsByCodigoDocente("DOC-001")).isTrue();
    }
}
