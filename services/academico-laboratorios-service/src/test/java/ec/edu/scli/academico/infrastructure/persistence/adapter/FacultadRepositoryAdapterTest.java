package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Facultad;
import ec.edu.scli.academico.infrastructure.persistence.entity.FacultadEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.FacultadEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.FacultadJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultadRepositoryAdapterTest {

    @Mock
    private FacultadJpaRepository facultadJpaRepository;

    private FacultadRepositoryAdapter facultadRepositoryAdapter;

    @BeforeEach
    void configurar() {
        facultadRepositoryAdapter =
                new FacultadRepositoryAdapter(facultadJpaRepository, new FacultadEntityMapper());
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Facultad facultad = Facultad.nueva("FISEI", "Facultad de Ingenieria", "Descripcion");
        UUID idGenerado = UUID.randomUUID();

        when(facultadJpaRepository.save(any(FacultadEntity.class))).thenAnswer(invocacion -> {
            FacultadEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Facultad resultado = facultadRepositoryAdapter.guardar(facultad);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("FISEI");
        assertThat(resultado.isActivo()).isTrue();

        ArgumentCaptor<FacultadEntity> captor = ArgumentCaptor.forClass(FacultadEntity.class);
        org.mockito.Mockito.verify(facultadJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("FISEI");
    }

    @Test
    void buscarPorId_deberiaRetornarFacultadMapeadaCuandoExiste() {

        UUID id = UUID.randomUUID();
        FacultadEntity entidad = new FacultadEntity();
        entidad.setId(id);
        entidad.setCodigo("FISEI");
        entidad.setNombre("Facultad de Ingenieria");
        entidad.setActivo(true);

        when(facultadJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Facultad> resultado = facultadRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(id);
        assertThat(resultado.get().getCodigo()).isEqualTo("FISEI");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(facultadJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Facultad> resultado = facultadRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeFacultadesMapeadas() {

        FacultadEntity entidad = new FacultadEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCodigo("FISEI");
        entidad.setNombre("Facultad de Ingenieria");

        Page<FacultadEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(facultadJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Facultad> resultado = facultadRepositoryAdapter.buscar("FISEI", null, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("FISEI");
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(facultadJpaRepository.existsByCodigo("FISEI")).thenReturn(true);

        boolean resultado = facultadRepositoryAdapter.existeCodigo("FISEI");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(facultadJpaRepository.existsByCodigoAndIdNot("FISEI", id)).thenReturn(false);

        boolean resultado = facultadRepositoryAdapter.existeCodigoParaOtroId("FISEI", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(facultadJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = facultadRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }
}