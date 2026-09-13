package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import ec.edu.scli.academico.infrastructure.persistence.entity.LaboratorioEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.LaboratorioEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.LaboratorioJpaRepository;
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
class LaboratorioRepositoryAdapterTest {

    @Mock
    private LaboratorioJpaRepository laboratorioJpaRepository;

    private LaboratorioRepositoryAdapter laboratorioRepositoryAdapter;

    private UUID pisoId;

    @BeforeEach
    void configurar() {
        laboratorioRepositoryAdapter =
                new LaboratorioRepositoryAdapter(laboratorioJpaRepository, new LaboratorioEntityMapper());
        pisoId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Laboratorio laboratorio = Laboratorio.nuevo(pisoId, "LAB-001", "Laboratorio de Redes", 30, null);
        UUID idGenerado = UUID.randomUUID();

        when(laboratorioJpaRepository.save(any(LaboratorioEntity.class))).thenAnswer(invocacion -> {
            LaboratorioEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Laboratorio resultado = laboratorioRepositoryAdapter.guardar(laboratorio);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("LAB-001");
        assertThat(resultado.getEstado()).isEqualTo(EstadoLaboratorio.DISPONIBLE);

        ArgumentCaptor<LaboratorioEntity> captor = ArgumentCaptor.forClass(LaboratorioEntity.class);
        org.mockito.Mockito.verify(laboratorioJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getPisoId()).isEqualTo(pisoId);
    }

    @Test
    void buscarPorId_deberiaRetornarLaboratorioMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        LaboratorioEntity entidad = new LaboratorioEntity();
        entidad.setId(id);
        entidad.setPisoId(pisoId);
        entidad.setCodigo("LAB-001");
        entidad.setNombre("Laboratorio de Redes");
        entidad.setCapacidad(30);
        entidad.setEstado(EstadoLaboratorio.DISPONIBLE);
        entidad.setActivo(true);

        when(laboratorioJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Laboratorio> resultado = laboratorioRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("LAB-001");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(laboratorioJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Laboratorio> resultado = laboratorioRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeLaboratoriosMapeados() {

        LaboratorioEntity entidad = new LaboratorioEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setPisoId(pisoId);
        entidad.setCodigo("LAB-001");
        entidad.setNombre("Laboratorio de Redes");
        entidad.setCapacidad(30);
        entidad.setEstado(EstadoLaboratorio.DISPONIBLE);

        Page<LaboratorioEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(laboratorioJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Laboratorio> resultado =
                laboratorioRepositoryAdapter.buscar("Redes", EstadoLaboratorio.DISPONIBLE, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("LAB-001");
    }

    @Test
    void buscarDisponibles_deberiaRetornarListaDeLaboratoriosDisponiblesYActivos() {

        LaboratorioEntity entidad = new LaboratorioEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setPisoId(pisoId);
        entidad.setCodigo("LAB-001");
        entidad.setNombre("Laboratorio de Redes");
        entidad.setCapacidad(30);
        entidad.setEstado(EstadoLaboratorio.DISPONIBLE);
        entidad.setActivo(true);

        when(laboratorioJpaRepository.findByEstadoAndActivoTrue(EstadoLaboratorio.DISPONIBLE))
                .thenReturn(List.of(entidad));

        List<Laboratorio> resultado = laboratorioRepositoryAdapter.buscarDisponibles();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoLaboratorio.DISPONIBLE);
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(laboratorioJpaRepository.existsByCodigo("LAB-001")).thenReturn(true);

        boolean resultado = laboratorioRepositoryAdapter.existeCodigo("LAB-001");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(laboratorioJpaRepository.existsByCodigoAndIdNot("LAB-001", id)).thenReturn(false);

        boolean resultado = laboratorioRepositoryAdapter.existeCodigoParaOtroId("LAB-001", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(laboratorioJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = laboratorioRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }

    @Test
    void contarPorEstado_deberiaDelegarEnElRepositorioJpa() {

        when(laboratorioJpaRepository.countByEstadoAndActivoTrue(EstadoLaboratorio.DISPONIBLE))
                .thenReturn(5L);

        long resultado = laboratorioRepositoryAdapter.contarPorEstado(EstadoLaboratorio.DISPONIBLE);

        assertThat(resultado).isEqualTo(5L);
    }
}