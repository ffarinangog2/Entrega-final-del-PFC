package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import ec.edu.scli.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface ReservaSpringDataRepository extends JpaRepository<ReservaJpaEntity, UUID>, JpaSpecificationExecutor<ReservaJpaEntity> {
    Optional<ReservaJpaEntity> findBySolicitudId(UUID solicitudId);
    boolean existsBySolicitudId(UUID solicitudId);
    Page<ReservaJpaEntity> findByLaboratorioId(UUID laboratorioId, Pageable pageable);
    Page<ReservaJpaEntity> findByResponsableId(UUID responsableId, Pageable pageable);
    Page<ReservaJpaEntity> findByLaboratorioIdAndFechaReservaBetween(UUID laboratorioId, LocalDate inicio, LocalDate fin, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<ReservaJpaEntity> findByIdForUpdate(@Param("id") UUID id);
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.laboratorioId = :laboratorioId AND r.fechaReserva = :fecha
              AND r.estado IN (ec.edu.scli.reservas.domain.model.EstadoReserva.PROGRAMADA,
                               ec.edu.scli.reservas.domain.model.EstadoReserva.EN_CURSO)
              AND :horaInicio < r.horaFin AND :horaFin > r.horaInicio
            """)
    long contarConflictosActivos(@Param("laboratorioId") UUID laboratorioId, @Param("fecha") LocalDate fecha,
                                 @Param("horaInicio") LocalTime horaInicio, @Param("horaFin") LocalTime horaFin);
}
