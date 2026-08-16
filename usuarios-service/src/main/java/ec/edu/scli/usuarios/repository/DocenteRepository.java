package ec.edu.scli.usuarios.repository;

import ec.edu.scli.usuarios.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {

    Optional<Docente> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoDocente(String codigoDocente);
}