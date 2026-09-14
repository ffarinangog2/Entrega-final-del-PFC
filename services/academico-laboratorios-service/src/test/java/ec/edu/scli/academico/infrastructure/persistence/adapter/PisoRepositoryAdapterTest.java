package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Piso;
import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.PisoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.PisoJpaRepository;
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
class PisoRepositoryAdapterTest {

    @Mock
    private PisoJpaRepository pisoJpaRepository;

    private PisoRepositoryAdapter pisoRepositoryAdapter;

    private UUID bloqueId;

    @BeforeEach
    void configurar() {
        pisoRepositoryAdapter = new PisoRepositoryAdapter(pisoJpaRepository, new PisoEntityMapper());
        bloqueId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Piso piso = Piso.nuevo(bloqueId, 2, "Piso de laboratorios de software");
        UUID idGenerado = UUID.randomUUID();

        when(pisoJpaRepository.save(any(PisoEntity.class))).thenAnswer(invocacion -> {
            PisoEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Piso resultado = pisoRepositoryAdapter.guardar(piso);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getNumero()).isEqualTo(2);
        assertThat(resultado.getBloqueId()).isEqualTo(bloqueId);

        ArgumentCaptor<PisoEntity> captor = ArgumentCaptor.forClass(PisoEntity.class);
        org.mockito.Mockito.verify(pisoJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getBloqueId()).isEqualTo(bloqueId);
    }

    @Test
    void buscarPorId_deberiaRetornarPisoMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        PisoEntity entidad = new PisoEntity();
        entidad.setId(id);
        entidad.setBloqueId(bloqueId);
        entidad.setNumero(2);
        entidad.setActivo(true);

        when(pisoJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Piso> resultado = pisoRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNumero()).isEqualTo(2);
        assertThat(resultado.get().getBloqueId()).isEqualTo(bloqueId);
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(pisoJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Piso> resultado = pisoRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDePisosMapeados() {

        PisoEntity entidad = new PisoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setBloqueId(bloqueId);
        entidad.setNumero(2);

        Page<PisoEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(pisoJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Piso> resultado = pisoRepositoryAdapter.buscar(bloqueId, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getNumero()).isEqualTo(2);
    }

    @Test
    void buscarPorBloque_deberiaRetornarListaDePisosMapeados() {

        PisoEntity entidad = new PisoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setBloqueId(bloqueId);
        entidad.setNumero(2);

        when(pisoJpaRepository.findByBloqueId(bloqueId)).thenReturn(List.of(entidad));

        List<Piso> resultado = pisoRepositoryAdapter.buscarPorBloque(bloqueId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNumero()).isEqualTo(2);
    }

    @Test
    void existeNumeroEnBloque_deberiaDelegarEnElRepositorioJpa() {

        when(pisoJpaRepository.existsByBloqueIdAndNumero(bloqueId, 2)).thenReturn(true);

        boolean resultado = pisoRepositoryAdapter.existeNumeroEnBloque(bloqueId, 2);

        assertThat(resultado).isTrue();
    }

    @Test
    void existeNumeroEnBloqueParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(pisoJpaRepository.existsByBloqueIdAndNumeroAndIdNot(bloqueId, 2, id)).thenReturn(false);

        boolean resultado = pisoRepositoryAdapter.existeNumeroEnBloqueParaOtroId(bloqueId, 2, id);

        assertThat(resultado).isFalse();
    }
}