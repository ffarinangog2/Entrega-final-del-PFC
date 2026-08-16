package ec.edu.scli.usuarios.repository;

import ec.edu.scli.usuarios.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdministradorRepository
        extends JpaRepository<Administrador, UUID> {

    Optional<Administrador> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoAdministrador(String codigoAdministrador);
}