package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.Equipo;
import ec.edu.scli.academico.enums.EstadoEquipo;
import ec.edu.scli.academico.infrastructure.persistence.entity.EquipoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.EquipoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.EquipoJpaRepository;
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
class EquipoRepositoryAdapterTest {

    @Mock
    private EquipoJpaRepository equipoJpaRepository;

    private EquipoRepositoryAdapter equipoRepositoryAdapter;

    private UUID laboratorioId;
    private UUID tipoEquipoId;

    @BeforeEach
    void configurar() {
        equipoRepositoryAdapter = new EquipoRepositoryAdapter(equipoJpaRepository, new EquipoEntityMapper());
        laboratorioId = UUID.randomUUID();
        tipoEquipoId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        Equipo equipo = Equipo.nuevo(
                laboratorioId, tipoEquipoId, "INV-001", "SN-001",
                "Dell", "OptiPlex", "i7", "16GB", "512GB",
                "192.168.1.10", "AA:BB:CC:DD:EE:FF", "Sin observaciones");
        UUID idGenerado = UUID.randomUUID();

        when(equipoJpaRepository.save(any(EquipoEntity.class))).thenAnswer(invocacion -> {
            EquipoEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        Equipo resultado = equipoRepositoryAdapter.guardar(equipo);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getCodigoInventario()).isEqualTo("INV-001");
        assertThat(resultado.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);

        ArgumentCaptor<EquipoEntity> captor = ArgumentCaptor.forClass(EquipoEntity.class);
        org.mockito.Mockito.verify(equipoJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigoInventario()).isEqualTo("INV-001");
    }

    @Test
    void buscarPorId_deberiaRetornarEquipoMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        EquipoEntity entidad = new EquipoEntity();
        entidad.setId(id);
        entidad.setLaboratorioId(laboratorioId);
        entidad.setTipoEquipoId(tipoEquipoId);
        entidad.setCodigoInventario("INV-001");
        entidad.setEstado(EstadoEquipo.OPERATIVO);
        entidad.setActivo(true);

        when(equipoJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<Equipo> resultado = equipoRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigoInventario()).isEqualTo("INV-001");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(equipoJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<Equipo> resultado = equipoRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscar_deberiaRetornarPaginaDeEquiposMapeados() {

        EquipoEntity entidad = new EquipoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setLaboratorioId(laboratorioId);
        entidad.setTipoEquipoId(tipoEquipoId);
        entidad.setCodigoInventario("INV-001");
        entidad.setEstado(EstadoEquipo.OPERATIVO);

        Page<EquipoEntity> paginaEntidades = new PageImpl<>(List.of(entidad));
        Pageable pageable = PageRequest.of(0, 10);

        when(equipoJpaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(paginaEntidades);

        Page<Equipo> resultado =
                equipoRepositoryAdapter.buscar(laboratorioId, EstadoEquipo.OPERATIVO, true, pageable);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getContent().get(0).getCodigoInventario()).isEqualTo("INV-001");
    }

    @Test
    void buscarPorLaboratorio_deberiaRetornarListaDeEquiposMapeados() {

        EquipoEntity entidad = new EquipoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setLaboratorioId(laboratorioId);
        entidad.setTipoEquipoId(tipoEquipoId);
        entidad.setCodigoInventario("INV-001");
        entidad.setEstado(EstadoEquipo.OPERATIVO);

        when(equipoJpaRepository.findByLaboratorioId(laboratorioId)).thenReturn(List.of(entidad));

        List<Equipo> resultado = equipoRepositoryAdapter.buscarPorLaboratorio(laboratorioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigoInventario()).isEqualTo("INV-001");
    }

    @Test
    void existeCodigoInventario_deberiaDelegarEnElRepositorioJpa() {

        when(equipoJpaRepository.existsByCodigoInventario("INV-001")).thenReturn(true);

        boolean resultado = equipoRepositoryAdapter.existeCodigoInventario("INV-001");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeCodigoInventarioParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(equipoJpaRepository.existsByCodigoInventarioAndIdNot("INV-001", id)).thenReturn(false);

        boolean resultado = equipoRepositoryAdapter.existeCodigoInventarioParaOtroId("INV-001", id);

        assertThat(resultado).isFalse();
    }

    @Test
    void existeNumeroSerie_deberiaDelegarEnElRepositorioJpa() {

        when(equipoJpaRepository.existsByNumeroSerie("SN-001")).thenReturn(true);

        boolean resultado = equipoRepositoryAdapter.existeNumeroSerie("SN-001");

        assertThat(resultado).isTrue();
    }

    @Test
    void existeNumeroSerieParaOtroId_deberiaDelegarEnElRepositorioJpa() {

        UUID id = UUID.randomUUID();
        when(equipoJpaRepository.existsByNumeroSerieAndIdNot("SN-001", id)).thenReturn(false);

        boolean resultado = equipoRepositoryAdapter.existeNumeroSerieParaOtroId("SN-001", id);

        assertThat(resultado).isFalse();
    }
}