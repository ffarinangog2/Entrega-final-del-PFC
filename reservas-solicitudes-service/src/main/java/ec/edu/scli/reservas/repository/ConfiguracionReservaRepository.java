package ec.edu.scli.reservas.repository;

import ec.edu.scli.reservas.entity.ConfiguracionReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositorio JPA para la configuración de las reservas. */
public interface ConfiguracionReservaRepository extends JpaRepository<ConfiguracionReserva, UUID> {

    Optional<ConfiguracionReserva> findByActivoTrue();
}
