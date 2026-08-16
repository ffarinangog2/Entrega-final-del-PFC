package ec.edu.scli.usuarios.repository;

import ec.edu.scli.usuarios.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepository
        extends JpaRepository<Perfil, UUID>,
        JpaSpecificationExecutor<Perfil> {

    Optional<Perfil> findByIdentificacion(String identificacion);

    Optional<Perfil> findByEmailInstitucional(String emailInstitucional);

    boolean existsByIdentificacion(String identificacion);

    boolean existsByEmailInstitucional(String emailInstitucional);
}