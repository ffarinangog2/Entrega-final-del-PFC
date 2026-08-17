package ec.edu.scli.reservas.repository;

import ec.edu.scli.reservas.entity.BloqueoAgenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Repositorio JPA para la persistencia y consulta de bloqueos de agenda. */
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, UUID>,
        JpaSpecificationExecutor<BloqueoAgenda> {

    Page<BloqueoAgenda> findByLaboratorioIdAndFechaAndActivoTrue(
            UUID laboratorioId, LocalDate fecha, Pageable pageable);

    Page<BloqueoAgenda> findByLaboratorioId(UUID laboratorioId, Pageable pageable);

    /** Cuenta bloqueos activos cuyos horarios se solapan con el intervalo indicado. */
    @Query("""
            SELECT COUNT(b)
            FROM BloqueoAgenda b
            WHERE b.laboratorioId = :laboratorioId
              AND b.fecha = :fecha
              AND b.activo = true
              AND :horaInicio < b.horaFin
              AND :horaFin > b.horaInicio
            """)
    long contarBloqueosActivosConflictivos(
            @Param("laboratorioId") UUID laboratorioId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
