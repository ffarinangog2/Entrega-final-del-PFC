package ec.edu.uteq.scli.auth_service.repository;

import ec.edu.uteq.scli.auth_service.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}