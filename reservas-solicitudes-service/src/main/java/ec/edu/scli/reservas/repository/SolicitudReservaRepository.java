package ec.edu.scli.reservas.repository;

import ec.edu.scli.reservas.entity.SolicitudReserva;
import ec.edu.scli.reservas.enums.EstadoSolicitud;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Repositorio JPA para la persistencia y consulta de solicitudes de reserva. */
public interface SolicitudReservaRepository extends JpaRepository<SolicitudReserva, UUID>,
        JpaSpecificationExecutor<SolicitudReserva> {

    Page<SolicitudReserva> findBySolicitanteId(UUID solicitanteId, Pageable pageable);

    Page<SolicitudReserva> findByEstado(EstadoSolicitud estado, Pageable pageable);

    Page<SolicitudReserva> findByLaboratorioIdAndFechaReserva(
            UUID laboratorioId, LocalDate fechaReserva, Pageable pageable);

    Optional<SolicitudReserva> findByClaveIdempotencia(String claveIdempotencia);

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    /** Obtiene una solicitud aplicando un bloqueo pesimista de escritura. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudReserva s WHERE s.id = :id")
    Optional<SolicitudReserva> findByIdForUpdate(@Param("id") UUID id);
}
