package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Carrera;
import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.CarreraEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.CarreraJpaRepository;
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
class CarreraRepositoryAdapterTest {

    @Mock
    private CarreraJpaRepository carreraJpaRepository;

    private CarreraRepositoryAdapter carreraRepositoryAdapter;

    private UUID facultadId;

    @BeforeEach
    void configurar() {
        carreraRepositoryAdapter = new CarreraRepositoryAdapter(carreraJpaRepository, new CarreraEntityMapper());
        facultadId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Carrera carrera = Carrera.nueva(facultadId, "SOFT", "Ingenieria de Software", "Descripcion");
        UUID idGenerado = UUID.randomUUID();

        when(carreraJpaRepository.save(any(CarreraEntity.class))).thenAnswer(invocacion -> {
            CarreraEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Carrera resultado = carreraRepositoryAdapter.guardar(carrera);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("SOFT");
        assertThat(resultado.getFacultadId()).isEqualTo(facultadId);

        ArgumentCaptor<CarreraEntity> captor = ArgumentCaptor.forClass(CarreraEntity.class);
        org.mockito.Mockito.verify(carreraJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getFacultadId()).isEqualTo(facultadId);
    }

    @Test
    void buscarPorId_deberiaRetornarCarreraMapeadaCuandoExiste() {

        UUID id = UUID.randomUUID();
        CarreraEntity entidad = new CarreraEntity();
        entidad.setId(id);
        entidad.setFacultadId(facultadId);
        entidad.setCodigo("SOFT");
        entidad.setNombre("Ingenieria de Software");
        entidad.setActivo(true);

        when(carreraJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Carrera> resultado = carreraRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("SOFT");
        assertThat(resultado.get().getFacultadId()).isEqualTo(facultadId);
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(carreraJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Carrera> resultado = carreraRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeCarrerasMapeadas() {

        CarreraEntity entidad = new CarreraEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setFacultadId(facultadId);
        entidad.setCodigo("SOFT");
        entidad.setNombre("Ingenieria de Software");

        Page<CarreraEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(carreraJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Carrera> resultado = carreraRepositoryAdapter.buscar(facultadId, "SOFT", null, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("SOFT");
    }

    @Test
    void buscarPorFacultad_deberiaRetornarListaDeCarrerasMapeadas() {

        CarreraEntity entidad = new CarreraEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setFacultadId(facultadId);
        entidad.setCodigo("SOFT");
        entidad.setNombre("Ingenieria de Software");

        when(carreraJpaRepository.findByFacultadId(facultadId)).thenReturn(List.of(entidad));

        List<Carrera> resultado = carreraRepositoryAdapter.buscarPorFacultad(facultadId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("SOFT");
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(carreraJpaRepository.existsByCodigo("SOFT")).thenReturn(true);

        boolean resultado = carreraRepositoryAdapter.existeCodigo("SOFT");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(carreraJpaRepository.existsByCodigoAndIdNot("SOFT", id)).thenReturn(false);

        boolean resultado = carreraRepositoryAdapter.existeCodigoParaOtroId("SOFT", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorFacultad_deberiaDelegarEnElRepositorioJpa() {

        when(carreraJpaRepository.existsByFacultadId(facultadId)).thenReturn(true);

        boolean resultado = carreraRepositoryAdapter.existePorFacultad(facultadId);

        assertThat(resultado).isTrue();
    }
}