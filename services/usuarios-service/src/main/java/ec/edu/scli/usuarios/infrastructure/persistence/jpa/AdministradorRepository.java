package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AdministradorRepository
        extends JpaRepository<Administrador, UUID> {

    Optional<Administrador> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoAdministrador(String codigoAdministrador);

    List<Administrador> findByPisoIdAndActivoTrue(UUID pisoId);
}
