package ec.edu.scli.academico.infrastructure.persistence.adapter;

import ec.edu.scli.academico.domain.model.HorarioAcademico;
import ec.edu.scli.academico.enums.DiaSemana;
import ec.edu.scli.academico.infrastructure.persistence.entity.HorarioAcademicoEntity;
import ec.edu.scli.academico.infrastructure.persistence.mapper.HorarioAcademicoEntityMapper;
import ec.edu.scli.academico.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioAcademicoRepositoryAdapterTest {

    @Mock
    private HorarioAcademicoJpaRepository horarioAcademicoJpaRepository;

    private HorarioAcademicoRepositoryAdapter horarioAcademicoRepositoryAdapter;

    private UUID materiaId;
    private UUID periodoLectivoId;
    private UUID laboratorioId;
    private UUID docenteId;

    @BeforeEach
    void configurar() {
        horarioAcademicoRepositoryAdapter = new HorarioAcademicoRepositoryAdapter(
                horarioAcademicoJpaRepository, new HorarioAcademicoEntityMapper());
        materiaId = UUID.randomUUID();
        periodoLectivoId = UUID.randomUUID();
        laboratorioId = UUID.randomUUID();
        docenteId = UUID.randomUUID();
    }

    @Test
    void guardar_deberiaMapearDominioAEntidadYDeVueltaADominio() {

        HorarioAcademico horario = HorarioAcademico.nuevo(
                materiaId, periodoLectivoId, laboratorioId, docenteId,
                DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0), "A");
        UUID idGenerado = UUID.randomUUID();

        when(horarioAcademicoJpaRepository.save(any(HorarioAcademicoEntity.class))).thenAnswer(invocacion -> {
            HorarioAcademicoEntity entidad = invocacion.getArgument(0);
            entidad.setId(idGenerado);
            entidad.setCreadoEn(OffsetDateTime.now());
            entidad.setActualizadoEn(OffsetDateTime.now());
            return entidad;
        });

        HorarioAcademico resultado = horarioAcademicoRepositoryAdapter.guardar(horario);

        assertThat(resultado.getId()).isEqualTo(idGenerado);
        assertThat(resultado.getParalelo()).isEqualTo("A");
        assertThat(resultado.getDiaSemana()).isEqualTo(DiaSemana.LUNES);

        ArgumentCaptor<HorarioAcademicoEntity> captor = ArgumentCaptor.forClass(HorarioAcademicoEntity.class);
        org.mockito.Mockito.verify(horarioAcademicoJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getMateriaId()).isEqualTo(materiaId);
    }

    @Test
    void buscarPorId_deberiaRetornarHorarioMapeadoCuandoExiste() {

        UUID id = UUID.randomUUID();
        HorarioAcademicoEntity entidad = new HorarioAcademicoEntity();
        entidad.setId(id);
        entidad.setMateriaId(materiaId);
        entidad.setPeriodoLectivoId(periodoLectivoId);
        entidad.setLaboratorioId(laboratorioId);
        entidad.setDocenteId(docenteId);
        entidad.setDiaSemana(DiaSemana.LUNES);
        entidad.setHoraInicio(LocalTime.of(8, 0));
        entidad.setHoraFin(LocalTime.of(10, 0));
        entidad.setParalelo("A");
        entidad.setActivo(true);

        when(horarioAcademicoJpaRepository.findById(id)).thenReturn(Optional.of(entidad));

        Optional<HorarioAcademico> resultado = horarioAcademicoRepositoryAdapter.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getParalelo()).isEqualTo("A");
    }

    @Test
    void buscarPorId_deberiaRetornarVacioCuandoNoExiste() {

        UUID idInexistente = UUID.randomUUID();
        when(horarioAcademicoJpaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Optional<HorarioAcademico> resultado = horarioAcademicoRepositoryAdapter.buscarPorId(idInexistente);

        assertThat(resultado).isEmpty();
    }

    @Test
    void buscarTodos_deberiaRetornarListaCompletaMapeada() {

        HorarioAcademicoEntity entidad = new HorarioAcademicoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setMateriaId(materiaId);
        entidad.setPeriodoLectivoId(periodoLectivoId);
        entidad.setLaboratorioId(laboratorioId);
        entidad.setDocenteId(docenteId);
        entidad.setDiaSemana(DiaSemana.MARTES);
        entidad.setHoraInicio(LocalTime.of(10, 0));
        entidad.setHoraFin(LocalTime.of(12, 0));
        entidad.setParalelo("B");

        when(horarioAcademicoJpaRepository.findAll()).thenReturn(List.of(entidad));

        List<HorarioAcademico> resultado = horarioAcademicoRepositoryAdapter.buscarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getParalelo()).isEqualTo("B");
    }

    @Test
    void buscarPorDocente_deberiaRetornarHorariosDelDocenteMapeados() {

        HorarioAcademicoEntity entidad = new HorarioAcademicoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setMateriaId(materiaId);
        entidad.setPeriodoLectivoId(periodoLectivoId);
        entidad.setDocenteId(docenteId);
        entidad.setDiaSemana(DiaSemana.MIERCOLES);
        entidad.setHoraInicio(LocalTime.of(14, 0));
        entidad.setHoraFin(LocalTime.of(16, 0));
        entidad.setParalelo("C");

        when(horarioAcademicoJpaRepository.findByDocenteId(docenteId)).thenReturn(List.of(entidad));

        List<HorarioAcademico> resultado = horarioAcademicoRepositoryAdapter.buscarPorDocente(docenteId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDocenteId()).isEqualTo(docenteId);
    }

    @Test
    void buscarPorLaboratorio_deberiaRetornarHorariosDelLaboratorioMapeados() {

        HorarioAcademicoEntity entidad = new HorarioAcademicoEntity();
        entidad.setId(UUID.randomUUID());
        entidad.setMateriaId(materiaId);
        entidad.setPeriodoLectivoId(periodoLectivoId);
        entidad.setLaboratorioId(laboratorioId);
        entidad.setDocenteId(docenteId);
        entidad.setDiaSemana(DiaSemana.JUEVES);
        entidad.setHoraInicio(LocalTime.of(8, 0));
        entidad.setHoraFin(LocalTime.of(10, 0));
        entidad.setParalelo("D");

        when(horarioAcademicoJpaRepository.findByLaboratorioId(laboratorioId)).thenReturn(List.of(entidad));

        List<HorarioAcademico> resultado = horarioAcademicoRepositoryAdapter.buscarPorLaboratorio(laboratorioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLaboratorioId()).isEqualTo(laboratorioId);
    }
}