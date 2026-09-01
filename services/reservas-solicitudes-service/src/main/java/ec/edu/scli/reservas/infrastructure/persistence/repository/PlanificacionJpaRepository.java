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

public interface PlanificacionJpaRepository extends JpaRepository<PlanificacionJpaEntity, UUID> {
    List<PlanificacionJpaEntity> findByCarreraId(UUID carreraId);

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
}
