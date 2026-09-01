package ec.edu.scli.academico.infrastructure.bootstrap;

import ec.edu.scli.academico.infrastructure.persistence.entity.CampusEntity;
import ec.edu.scli.academico.infrastructure.persistence.repository.BloqueJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.CampusJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.CarreraJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.EquipoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.FacultadJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.HorarioAcademicoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.LaboratorioJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.MateriaJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.PeriodoLectivoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.PisoJpaRepository;
import ec.edu.scli.academico.infrastructure.persistence.repository.TipoEquipoJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialAcademicDataBootstrapTest {

    @Test
    void creaDatosDeterministasConPersistYLaSegundaEjecucionEsIdempotente() {
        CampusJpaRepository campuses = mock(CampusJpaRepository.class);
        BloqueJpaRepository blocks = mock(BloqueJpaRepository.class);
        PisoJpaRepository floors = mock(PisoJpaRepository.class);
        FacultadJpaRepository faculties = mock(FacultadJpaRepository.class);
        CarreraJpaRepository careers = mock(CarreraJpaRepository.class);
        PeriodoLectivoJpaRepository periods = mock(PeriodoLectivoJpaRepository.class);
        MateriaJpaRepository subjects = mock(MateriaJpaRepository.class);
        LaboratorioJpaRepository labs = mock(LaboratorioJpaRepository.class);
        TipoEquipoJpaRepository equipmentTypes = mock(TipoEquipoJpaRepository.class);
        EquipoJpaRepository equipment = mock(EquipoJpaRepository.class);
        HorarioAcademicoJpaRepository schedules = mock(HorarioAcademicoJpaRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        InitialAcademicDataBootstrap bootstrap = new InitialAcademicDataBootstrap(
                campuses, blocks, floors, faculties, careers, periods, subjects, labs,
                equipmentTypes, equipment, schedules, entityManager);

        bootstrap.run(null);

        var entities = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(entityManager, org.mockito.Mockito.times(43)).persist(entities.capture());
        var ids = entities.getAllValues().stream()
                .map(InitialAcademicDataBootstrapTest::entityId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(43, ids.size());
        assertTrue(ids.contains(InitialAcademicDataBootstrap.CAMPUS_ID));
        assertTrue(ids.contains(InitialAcademicDataBootstrap.BLOCK_ID));
        assertTrue(ids.contains(InitialAcademicDataBootstrap.CAREER_2_ID));
        verify(campuses, never()).save(any(CampusEntity.class));

        when(campuses.existsById(any())).thenReturn(true);
        when(blocks.existsById(any())).thenReturn(true);
        when(floors.existsById(any())).thenReturn(true);
        when(faculties.existsById(any())).thenReturn(true);
        when(careers.existsById(any())).thenReturn(true);
        when(periods.existsById(any())).thenReturn(true);
        when(subjects.existsById(any())).thenReturn(true);
        when(labs.existsById(any())).thenReturn(true);
        when(equipmentTypes.existsById(any())).thenReturn(true);
        when(equipment.existsById(any())).thenReturn(true);
        when(schedules.existsById(any())).thenReturn(true);
        clearInvocations(entityManager);

        bootstrap.run(null);

        verify(entityManager, never()).persist(any());
    }

    private static UUID entityId(Object entity) {
        try {
            return (UUID) entity.getClass().getMethod("getId").invoke(entity);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Entidad de bootstrap sin getId", exception);
        }
    }
}
