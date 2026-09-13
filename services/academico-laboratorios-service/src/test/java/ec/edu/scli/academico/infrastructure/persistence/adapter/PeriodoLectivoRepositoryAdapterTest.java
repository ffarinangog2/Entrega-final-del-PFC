package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.PeriodoLectivoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.PeriodoLectivoJpaRepository;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodoLectivoRepositoryAdapterTest {

    @Mock
    private PeriodoLectivoJpaRepository periodoLectivoJpaRepository;

    private PeriodoLectivoRepositoryAdapter periodoLectivoRepositoryAdapter;

    @BeforeEach
    void configurar() {
        periodoLectivoRepositoryAdapter =
                new PeriodoLectivoRepositoryAdapter(periodoLectivoJpaRepository, new PeriodoLectivoEntityMapper());
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        PeriodoLectivo periodo = PeriodoLectivo.nuevo(
                "2026-1", "Periodo Regular 2026-1",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31),
                EstadoPeriodo.PLANIFICADO);
        UUID idGenerado = UUID.randomUUID();

        when(periodoLectivoJpaRepository.save(any(PeriodoLectivoEntity.class))).thenAnswer(invocacion -> {
            PeriodoLectivoEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        PeriodoLectivo resultado = periodoLectivoRepositoryAdapter.guardar(periodo);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigo()).isEqualTo("2026-1");
        assertThat(resultado.getEstado()).isEqualTo(EstadoPeriodo.PLANIFICADO);

        ArgumentCaptor<PeriodoLectivoEntity> captor = ArgumentCaptor.forClass(PeriodoLectivoEntity.class);
        org.mockito.Mockito.verify(periodoLectivoJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigo()).isEqualTo("2026-1");
    }

    @Test
    void buscarPorId_deberiaRetornarPeriodoMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        PeriodoLectivoEntity entidad = new PeriodoLectivoEntity();
        entidad.setId(id);
        entidad.setCodigo("2026-1");
        entidad.setNombre("Periodo Regular 2026-1");
        entidad.setFechaInicio(LocalDate.of(2026, 3, 1));
        entidad.setFechaFin(LocalDate.of(2026, 7, 31));
        entidad.setEstado(EstadoPeriodo.PLANIFICADO);

        when(periodoLectivoJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<PeriodoLectivo> resultado = periodoLectivoRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("2026-1");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(periodoLectivoJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<PeriodoLectivo> resultado = periodoLectivoRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDePeriodosMapeados() {

        PeriodoLectivoEntity entidad = new PeriodoLectivoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCodigo("2026-1");
        entidad.setNombre("Periodo Regular 2026-1");
        entidad.setFechaInicio(LocalDate.of(2026, 3, 1));
        entidad.setFechaFin(LocalDate.of(2026, 7, 31));

        Page<PeriodoLectivoEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(periodoLectivoJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<PeriodoLectivo> resultado = periodoLectivoRepositoryAdapter.buscar("2026-1", pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigo()).isEqualTo("2026-1");
    }

    @Test
    void buscarActualPorEstado_deberiaRetornarElPeriodoMasRecienteConEseEstado() {

        PeriodoLectivoEntity entidad = new PeriodoLectivoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setCodigo("2026-1");
        entidad.setNombre("Periodo Regular 2026-1");
        entidad.setFechaInicio(LocalDate.of(2026, 3, 1));
        entidad.setFechaFin(LocalDate.of(2026, 7, 31));
        entidad.setEstado(EstadoPeriodo.ACTIVO);

        when(periodoLectivoJpaRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo.ACTIVO))
                .thenReturn(Optional.of(entidad));

        Optional<PeriodoLectivo> resultado =
                periodoLectivoRepositoryAdapter.buscarActualPorEstado(EstadoPeriodo.ACTIVO);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(EstadoPeriodo.ACTIVO);
    }

    @Test
    void buscarActualPorEstado_deberiaRetornarVacioCuandoNoHayNinguno() {

        when(periodoLectivoJpaRepository.findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo.ACTIVO))
                .thenReturn(Optional.empty());

        Optional<PeriodoLectivo> resultado =
                periodoLectivoRepositoryAdapter.buscarActualPorEstado(EstadoPeriodo.ACTIVO);

        assertThat(resultado).isEmpty();
    }

    @Test
    void existeCodigo_deberiaDelegarEnElRepositorioJpa() {

        when(periodoLectivoJpaRepository.existsByCodigo("2026-1")).thenReturn(true);

        boolean resultado = periodoLectivoRepositoryAdapter.existeCodigo("2026-1");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(periodoLectivoJpaRepository.existsByCodigoAndIdNot("2026-1", id)).thenReturn(false);

        boolean resultado = periodoLectivoRepositoryAdapter.existeCodigoParaOtroId("2026-1", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existePorId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(periodoLectivoJpaRepository.existsById(id)).thenReturn(true);

        boolean resultado = periodoLectivoRepositoryAdapter.existePorId(id);

        assertThat(resultado).isTrue();
    }
}