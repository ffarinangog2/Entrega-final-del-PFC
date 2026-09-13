package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Campus;
import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.CampusEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.CampusJpaRepository;
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
class CampusRepositoryAdapterTest {

    @Mock
    private CampusJpaRepository campusJpaRepository;

    private CampusRepositoryAdapter campusRepositoryAdapter;

    @BeforeEach
    void configurar() {
        campusRepositoryAdapter = new CampusRepositoryAdapter(campusJpaRepository, new CampusEntityMapper());
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Campus campus = Campus.nuevo("CENTRAL", "Campus Central", "Direccion");
        UUID idGenerado = UUID.randomUUID();

        when(campusJpaRepository.save(any(CampusEntity.class))).thenAnswer(invocacion -> {
            CampusEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Campus resultado = campusRepositoryAdapter.guardar(campus);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("CENTRAL");
        assertThat(resultado.isActivo()).isTrue();

        ArgumentCaptor<CampusEntity> captor = ArgumentCaptor.forClass(CampusEntity.class);
        org.mockito.Mockito.verify(campusJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("CENTRAL");
    }

    @Test
    void buscarPorId_deberiaRetornarCampusMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        CampusEntity entidad = new CampusEntity();
        entidad.setId(id);
        entidad.setCodigo("CENTRAL");
        entidad.setNombre("Campus Central");
        entidad.setActivo(true);

        when(campusJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Campus> resultado = campusRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(id);
        assertThat(resultado.get().getCodigo()).isEqualTo("CENTRAL");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(campusJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Campus> resultado = campusRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeCampusMapeados() {

        CampusEntity entidad = new CampusEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCodigo("CENTRAL");
        entidad.setNombre("Campus Central");

        Page<CampusEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(campusJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Campus> resultado = campusRepositoryAdapter.buscar("CENTRAL", null, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("CENTRAL");
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(campusJpaRepository.existsByCodigo("CENTRAL")).thenReturn(true);

        boolean resultado = campusRepositoryAdapter.existeCodigo("CENTRAL");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(campusJpaRepository.existsByCodigoAndIdNot("CENTRAL", id)).thenReturn(false);

        boolean resultado = campusRepositoryAdapter.existeCodigoParaOtroId("CENTRAL", id);

        assertThat(resultado).isFalse();
    }
}