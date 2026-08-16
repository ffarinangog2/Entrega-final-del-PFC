package ec.edu.uteq.scli.auth_service.repository;

import ec.edu.uteq.scli.auth_service.entity.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    @EntityGraph(attributePaths = "permisos")
    Optional<Rol> findWithPermisosByCodigoIgnoreCase(String codigo);
}