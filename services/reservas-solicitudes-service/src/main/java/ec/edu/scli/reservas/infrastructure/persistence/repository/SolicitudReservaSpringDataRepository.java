package ec.edu.scli.reservas.infrastructure.persistence.repository;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import ec.edu.scli.reservas.infrastructure.persistence.entity.SolicitudReservaJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.time.Instant;

public interface SolicitudReservaSpringDataRepository extends JpaRepository<SolicitudReservaJpaEntity, UUID>, JpaSpecificationExecutor<SolicitudReservaJpaEntity> {
    Page<SolicitudReservaJpaEntity> findBySolicitanteId(UUID solicitanteId, Pageable pageable);
    Page<SolicitudReservaJpaEntity> findByEstado(EstadoSolicitud estado, Pageable pageable);
    Optional<SolicitudReservaJpaEntity> findByClaveIdempotencia(String claveIdempotencia);
    List<SolicitudReservaJpaEntity> findByEstadoAndCreadaEnBefore(EstadoSolicitud estado, Instant limite);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudReserva s WHERE s.id = :id")
    Optional<SolicitudReservaJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT COUNT(s) FROM SolicitudReserva s
            WHERE s.docenteId = :docenteId AND s.fechaReserva = :fecha
              AND s.estado IN (ec.edu.scli.reservas.domain.model.EstadoSolicitud.PENDIENTE,
                               ec.edu.scli.reservas.domain.model.EstadoSolicitud.EN_REVISION,
                               ec.edu.scli.reservas.domain.model.EstadoSolicitud.PROPUESTA,
                               ec.edu.scli.reservas.domain.model.EstadoSolicitud.APROBADA)
              AND (:excluirId IS NULL OR s.id <> :excluirId)
              AND :horaInicio < s.horaFin AND :horaFin > s.horaInicio
            """)
    long contarConflictosDocente(
            @Param("docenteId") UUID docenteId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excluirId") UUID excluirId);
}
