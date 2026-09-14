package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.TipoEquipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.TipoEquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.TipoEquipoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.TipoEquipoJpaRepository;
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
class TipoEquipoRepositoryAdapterTest {

    @Mock
    private TipoEquipoJpaRepository tipoEquipoJpaRepository;

    private TipoEquipoRepositoryAdapter tipoEquipoRepositoryAdapter;

    @BeforeEach
    void configurar() {
        tipoEquipoRepositoryAdapter =
                new TipoEquipoRepositoryAdapter(tipoEquipoJpaRepository, new TipoEquipoEntityMapper());
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        TipoEquipo tipoEquipo = TipoEquipo.nuevo("PC-DESK", "Computador de escritorio", "Descripcion");
        UUID idGenerado = UUID.randomUUID();

        when(tipoEquipoJpaRepository.save(any(TipoEquipoEntity.class))).thenAnswer(invocacion -> {
            TipoEquipoEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        TipoEquipo resultado = tipoEquipoRepositoryAdapter.guardar(tipoEquipo);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("PC-DESK");
        assertThat(resultado.isActivo()).isTrue();

        ArgumentCaptor<TipoEquipoEntity> captor = ArgumentCaptor.forClass(TipoEquipoEntity.class);
        org.mockito.Mockito.verify(tipoEquipoJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("PC-DESK");
    }

    @Test
    void buscarPorId_deberiaRetornarTipoEquipoMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        TipoEquipoEntity entidad = new TipoEquipoEntity();
        entidad.setId(id);
        entidad.setCodigo("PC-DESK");
        entidad.setNombre("Computador de escritorio");
        entidad.setActivo(true);

        when(tipoEquipoJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<TipoEquipo> resultado = tipoEquipoRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("PC-DESK");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(tipoEquipoJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<TipoEquipo> resultado = tipoEquipoRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeTiposEquipoMapeados() {

        TipoEquipoEntity entidad = new TipoEquipoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCodigo("PC-DESK");
        entidad.setNombre("Computador de escritorio");

        Page<TipoEquipoEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(tipoEquipoJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<TipoEquipo> resultado = tipoEquipoRepositoryAdapter.buscar("PC-DESK", null, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("PC-DESK");
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(tipoEquipoJpaRepository.existsByCodigo("PC-DESK")).thenReturn(true);

        boolean resultado = tipoEquipoRepositoryAdapter.existeCodigo("PC-DESK");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(tipoEquipoJpaRepository.existsByCodigoAndIdNot("PC-DESK", id)).thenReturn(false);

        boolean resultado = tipoEquipoRepositoryAdapter.existeCodigoParaOtroId("PC-DESK", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(tipoEquipoJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = tipoEquipoRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }
}