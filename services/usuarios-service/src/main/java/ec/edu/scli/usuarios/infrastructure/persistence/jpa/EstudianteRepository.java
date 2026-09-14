package ec.edu.scli.usuarios.infrastructure.persistence.jpa;

import ec.edu.scli.usuarios.infrastructure.persistence.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    Optional<Estudiante> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByMatricula(String matricula);
}