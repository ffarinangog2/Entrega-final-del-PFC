package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {

    Optional<Docente> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoDocente(String codigoDocente);
}