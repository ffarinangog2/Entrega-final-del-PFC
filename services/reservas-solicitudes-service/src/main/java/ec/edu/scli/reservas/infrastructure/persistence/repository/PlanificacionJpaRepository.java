package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.domain.model.EstadoPlanificacion;
import ec.edu.scli.reservas.infrastructure.persistence.entity.PlanificacionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import ec.edu.scli.reservas.domain.model.EstadoPlanificacionAgregada;

public interface PlanificacionJpaRepository extends JpaRepository<PlanificacionJpaEntity, UUID> {
    List<PlanificacionJpaEntity> findByCarreraId(UUID carreraId);
    List<PlanificacionJpaEntity> findByPlanificacionId(UUID planificacionId);
    List<PlanificacionJpaEntity> findByDocenteIdAndDiaSemana(UUID docenteId, String diaSemana);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PlanificacionJpaEntity p
            where p.laboratorioId = :laboratorioId and p.diaSemana = :dia
              and p.estado = :estado and p.id <> :excluirId
              and p.horaInicio < :horaFin and p.horaFin > :horaInicio
            """)
    List<PlanificacionJpaEntity> bloquearConflictos(
            @Param("laboratorioId") UUID laboratorioId,
            @Param("dia") String dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("estado") EstadoPlanificacion estado,
            @Param("excluirId") UUID excluirId);

    boolean existsByLaboratorioIdAndDiaSemanaAndEstadoAndHoraInicioLessThanAndHoraFinGreaterThan(
            UUID laboratorioId, String diaSemana, EstadoPlanificacion estado,
            LocalTime horaFin, LocalTime horaInicio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from PlanificacionJpaEntity b, PlanificacionAgregadaJpaEntity a
            where b.planificacionId = a.id and a.periodoId = :periodoId
              and a.id <> :planificacionId and a.estado in :estados
              and b.estado <> ec.edu.scli.reservas.domain.model.EstadoPlanificacion.CANCELADA
              and b.diaSemana = :dia and b.horaInicio < :horaFin and b.horaFin > :horaInicio
              and ((:docenteId is not null and b.docenteId = :docenteId)
                   or b.laboratorioId = :laboratorioId)
            """)
    List<PlanificacionJpaEntity> bloquearConflictosGlobales(
            @Param("planificacionId") UUID planificacionId,
            @Param("periodoId") UUID periodoId,
            @Param("docenteId") UUID docenteId,
            @Param("laboratorioId") UUID laboratorioId,
            @Param("dia") String dia,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("estados") List<EstadoPlanificacionAgregada> estados);

    @Query("""
            select b from PlanificacionJpaEntity b, PlanificacionAgregadaJpaEntity a
            where b.planificacionId = a.id and a.periodoId = :periodoId and a.estado in :estados
              and (:planificacionId is null or a.id <> :planificacionId)
              and b.estado <> ec.edu.scli.reservas.domain.model.EstadoPlanificacion.CANCELADA
              and b.diaSemana = :dia and b.horaInicio < :horaFin and b.horaFin > :horaInicio
            """)
    List<PlanificacionJpaEntity> buscarOcupacionGlobal(
            @Param("planificacionId") UUID planificacionId, @Param("periodoId") UUID periodoId,
            @Param("dia") String dia, @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin, @Param("estados") List<EstadoPlanificacionAgregada> estados);
}
