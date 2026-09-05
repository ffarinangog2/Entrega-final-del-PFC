package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Materia;
import ec.edu.scli.academico.infrastructure.persistence.entity.MateriaEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.MateriaEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.MateriaJpaRepository;
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
class MateriaRepositoryAdapterTest {

    @Mock
    private MateriaJpaRepository materiaJpaRepository;

    private MateriaRepositoryAdapter materiaRepositoryAdapter;

    private UUID carreraId;

    @BeforeEach
    void configurar() {
        materiaRepositoryAdapter = new MateriaRepositoryAdapter(materiaJpaRepository, new MateriaEntityMapper());
        carreraId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Materia materia = Materia.nueva(carreraId, "PROG1", "Programacion I", 64);
        UUID idGenerado = UUID.randomUUID();

        when(materiaJpaRepository.save(any(MateriaEntity.class))).thenAnswer(invocacion -> {
            MateriaEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Materia resultado = materiaRepositoryAdapter.guardar(materia);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("PROG1");
        assertThat(resultado.getNumeroHoras()).isEqualTo(64);

        ArgumentCaptor<MateriaEntity> captor = ArgumentCaptor.forClass(MateriaEntity.class);
        org.mockito.Mockito.verify(materiaJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCarreraId()).isEqualTo(carreraId);
    }

    @Test
    void buscarPorId_deberiaRetornarMateriaMapeadaCuandoExiste() {

        UUID id = UUID.randomUUID();
        MateriaEntity entidad = new MateriaEntity();
        entidad.setId(id);
        entidad.setCarreraId(carreraId);
        entidad.setCodigo("PROG1");
        entidad.setNombre("Programacion I");
        entidad.setNumeroHoras(64);
        entidad.setActivo(true);

        when(materiaJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Materia> resultado = materiaRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("PROG1");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(materiaJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Materia> resultado = materiaRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeMateriasMapeadas() {

        MateriaEntity entidad = new MateriaEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCarreraId(carreraId);
        entidad.setCodigo("PROG1");
        entidad.setNombre("Programacion I");
        entidad.setNumeroHoras(64);

        Page<MateriaEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(materiaJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Materia> resultado = materiaRepositoryAdapter.buscar(carreraId, "PROG1", null, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("PROG1");
    }

    @Test
    void buscarPorCarrera_deberiaRetornarListaDeMateriasMapeadas() {

        MateriaEntity entidad = new MateriaEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCarreraId(carreraId);
        entidad.setCodigo("PROG1");
        entidad.setNombre("Programacion I");
        entidad.setNumeroHoras(64);

        when(materiaJpaRepository.findByCarreraId(carreraId)).thenReturn(List.of(entidad));

        List<Materia> resultado = materiaRepositoryAdapter.buscarPorCarrera(carreraId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("PROG1");
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(materiaJpaRepository.existsByCodigo("PROG1")).thenReturn(true);

        boolean resultado = materiaRepositoryAdapter.existeCodigo("PROG1");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(materiaJpaRepository.existsByCodigoAndIdNot("PROG1", id)).thenReturn(false);

        boolean resultado = materiaRepositoryAdapter.existeCodigoParaOtroId("PROG1", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(materiaJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = materiaRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }
}