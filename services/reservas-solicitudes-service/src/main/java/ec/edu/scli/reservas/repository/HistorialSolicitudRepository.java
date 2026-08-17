package ec.edu.scli.reservas.repository;

import ec.edu.scli.reservas.entity.HistorialSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio JPA para consultar el historial de las solicitudes de reserva. */
public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, UUID> {

    Page<HistorialSolicitud> findBySolicitudIdOrderByFechaHoraAsc(UUID solicitudId, Pageable pageable);
}
