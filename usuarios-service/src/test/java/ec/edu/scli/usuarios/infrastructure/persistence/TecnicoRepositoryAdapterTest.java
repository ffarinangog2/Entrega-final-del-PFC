package ec.edu.scli.usuarios.infrastructure.persistence;

import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.Tecnico;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;
import ec.edu.scli.usuarios.infrastructure.persistence.jpa.TecnicoRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecnicoRepositoryAdapterTest {

    @Mock
    private TecnicoRepository repository;

    @Mock
    private EntityManager entityManager;

    private TecnicoRepositoryAdapter adapter;

    private UUID perfilId;
    private UUID tecnicoId;

    @BeforeEach
    void setUp() {
        PerfilRelacionadoPersistenceMapper mapper =
                new PerfilRelacionadoPersistenceMapper(new PerfilPersistenceMapper());

        adapter = new TecnicoRepositoryAdapter(repository, mapper);
        ReflectionTestUtils.setField(adapter, "entityManager", entityManager);

        perfilId = UUID.randomUUID();
        tecnicoId = UUID.randomUUID();
    }

    private ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entidad() {
        ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil perfilEntity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil();
        perfilEntity.setId(perfilId);
        perfilEntity.setActivo(true);

        ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico entity =
                new ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico();
        entity.setId(tecnicoId);
        entity.setPerfil(perfilEntity);
        entity.setCodigoTecnico("TEC-001");
        entity.setActivo(true);
        return entity;
    }

    private Tecnico dominio() {
        Perfil perfil = new Perfil();
        perfil.setId(perfilId);

        Tecnico tecnico = new Tecnico();
        tecnico.setId(tecnicoId);
        tecnico.setPerfil(perfil);
        tecnico.setCodigoTecnico("TEC-001");
        tecnico.setActivo(true);
        return tecnico;
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

        Tecnico guardado = adapter.save(dominio());

        assertThat(guardado.getId()).isEqualTo(tecnicoId);
        assertThat(guardado.getCodigoTecnico()).isEqualTo("TEC-001");
    }

    @Test
    void findAll_conPaginacion_deberiaMapearPageResult() {
        Page<ec.edu.scli.usuarios.infrastructure.persistence.entity.Tecnico> pagina =
                new PageImpl<>(List.of(entidad()), PageRequest.of(0, 10), 1);

        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        PageResult<Tecnico> resultado =
                adapter.findAll(new PageCriteria(0, 10, List.of()));

        assertThat(resultado.content()).hasSize(1);
    }

    @Test
    void findAll_sinArgumentos_deberiaMapearListaCompleta() {
        when(repository.findAll()).thenReturn(List.of(entidad()));

        List<Tecnico> resultado = adapter.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(tecnicoId);
    }

    @Test
    void findById_deberiaRetornarTecnico_cuandoExiste() {
        when(repository.findById(tecnicoId)).thenReturn(Optional.of(entidad()));

        Optional<Tecnico> resultado = adapter.findById(tecnicoId);

        assertThat(resultado).isPresent();
    }

    @Test
    void findByPerfilId_deberiaRetornarTecnico_cuandoExiste() {
        when(repository.findByPerfilId(perfilId)).thenReturn(Optional.of(entidad()));

        Optional<Tecnico> resultado = adapter.findByPerfilId(perfilId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getPerfil().getId()).isEqualTo(perfilId);
    }

    @Test
    void existsByPerfilId_deberiaDelegarEnRepository() {
        when(repository.existsByPerfilId(perfilId)).thenReturn(true);

        assertThat(adapter.existsByPerfilId(perfilId)).isTrue();
    }

    @Test
    void existsByCodigoTecnico_deberiaDelegarEnRepository() {
        when(repository.existsByCodigoTecnico("TEC-001")).thenReturn(true);

        assertThat(adapter.existsByCodigoTecnico("TEC-001")).isTrue();
    }
}
