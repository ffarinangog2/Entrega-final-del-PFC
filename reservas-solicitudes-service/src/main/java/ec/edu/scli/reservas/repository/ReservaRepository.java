package ec.edu.scli.reservas.repository;

import ec.edu.scli.reservas.entity.Reserva;
import ec.edu.scli.reservas.enums.EstadoReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/** Repositorio JPA para la persistencia y consulta de reservas. */
public interface ReservaRepository extends JpaRepository<Reserva, UUID>, JpaSpecificationExecutor<Reserva> {

    Optional<Reserva> findBySolicitudId(UUID solicitudId);

    boolean existsBySolicitudId(UUID solicitudId);

    Page<Reserva> findByLaboratorioId(UUID laboratorioId, Pageable pageable);

    Page<Reserva> findByResponsableId(UUID responsableId, Pageable pageable);

    Page<Reserva> findByEstado(EstadoReserva estado, Pageable pageable);

    Page<Reserva> findByLaboratorioIdAndFechaReservaBetween(
            UUID laboratorioId, LocalDate fechaInicio, LocalDate fechaFin, Pageable pageable);

    /** Obtiene una reserva aplicando un bloqueo pesimista de escritura. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") UUID id);

    /** Cuenta reservas activas cuyos horarios se solapan con el intervalo indicado. */
    @Query("""
            SELECT COUNT(r)
            FROM Reserva r
            WHERE r.laboratorioId = :laboratorioId
              AND r.fechaReserva = :fecha
              AND r.estado IN (ec.edu.scli.reservas.enums.EstadoReserva.PROGRAMADA,
                               ec.edu.scli.reservas.enums.EstadoReserva.EN_CURSO)
              AND :horaInicio < r.horaFin
              AND :horaFin > r.horaInicio
            """)
    long contarConflictosActivos(
            @Param("laboratorioId") UUID laboratorioId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
