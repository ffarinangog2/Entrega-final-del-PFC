package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/* Antes
public interface PerfilRepository
        extends JpaRepository<Perfil, UUID>,
        JpaSpecificationExecutor<Perfil> {

    Optional<Perfil> findByIdentificacion(String identificacion);

    Optional<Perfil> findByEmailInstitucional(String emailInstitucional);

    boolean existsByIdentificacion(String identificacion);

    boolean existsByEmailInstitucional(String emailInstitucional);
}*/


//Nuevo
public interface PerfilRepository
        extends JpaRepository<Perfil, UUID>,
        JpaSpecificationExecutor<Perfil> {

    Optional<Perfil> findByIdentificacionHash(String identificacionHash);

    Optional<Perfil> findByEmailInstitucional(String emailInstitucional);

    boolean existsByIdentificacionHash(String identificacionHash);

    boolean existsByEmailInstitucional(String emailInstitucional);
}