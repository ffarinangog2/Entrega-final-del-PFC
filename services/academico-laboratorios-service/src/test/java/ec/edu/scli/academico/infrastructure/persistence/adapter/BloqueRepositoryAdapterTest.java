package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Bloque;
import ec.edu.scli.academico.infrastructure.persistence.entity.BloqueEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.BloqueEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.BloqueJpaRepository;
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
class BloqueRepositoryAdapterTest {

    @Mock
    private BloqueJpaRepository bloqueJpaRepository;

    private BloqueRepositoryAdapter bloqueRepositoryAdapter;

    private UUID campusId;

    @BeforeEach
    void configurar() {
        bloqueRepositoryAdapter = new BloqueRepositoryAdapter(bloqueJpaRepository, new BloqueEntityMapper());
        campusId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Bloque bloque = Bloque.nuevo(campusId, "BLQ-A", "Bloque A");
        UUID idGenerado = UUID.randomUUID();

        when(bloqueJpaRepository.save(any(BloqueEntity.class))).thenAnswer(invocacion -> {
            BloqueEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Bloque resultado = bloqueRepositoryAdapter.guardar(bloque);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("BLQ-A");
        assertThat(resultado.getCampusId()).isEqualTo(campusId);

        ArgumentCaptor<BloqueEntity> captor = ArgumentCaptor.forClass(BloqueEntity.class);
        org.mockito.Mockito.verify(bloqueJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCampusId()).isEqualTo(campusId);
    }

    @Test
    void buscarPorId_deberiaRetornarBloqueMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        BloqueEntity entidad = new BloqueEntity();
        entidad.setId(id);
        entidad.setCampusId(campusId);
        entidad.setCodigo("BLQ-A");
        entidad.setNombre("Bloque A");
        entidad.setActivo(true);

        when(bloqueJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Bloque> resultado = bloqueRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("BLQ-A");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(bloqueJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Bloque> resultado = bloqueRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeBloquesMapeados() {

        BloqueEntity entidad = new BloqueEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCampusId(campusId);
        entidad.setCodigo("BLQ-A");
        entidad.setNombre("Bloque A");

        Page<BloqueEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(bloqueJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Bloque> resultado = bloqueRepositoryAdapter.buscar(campusId, "Bloque", true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("BLQ-A");
    }

    @Test
    void buscarPorCampus_deberiaRetornarListaDeBloquesMapeados() {

        BloqueEntity entidad = new BloqueEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCampusId(campusId);
        entidad.setCodigo("BLQ-A");
        entidad.setNombre("Bloque A");

        when(bloqueJpaRepository.findByCampusId(campusId)).thenReturn(List.of(entidad));

        List<Bloque> resultado = bloqueRepositoryAdapter.buscarPorCampus(campusId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("BLQ-A");
    }

    @Test
    void existeCodigoEnCampus_deberiaDelegarEnElRepositorioJpa() {

        when(bloqueJpaRepository.existsByCampusIdAndCodigo(campusId, "BLQ-A")).thenReturn(true);

        boolean resultado = bloqueRepositoryAdapter.existeCodigoEnCampus(campusId, "BLQ-A");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoEnCampusParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(bloqueJpaRepository.existsByCampusIdAndCodigoAndIdNot(campusId, "BLQ-A", id)).thenReturn(false);

        boolean resultado = bloqueRepositoryAdapter.existeCodigoEnCampusParaOtroId(campusId, "BLQ-A", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(bloqueJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = bloqueRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }
}