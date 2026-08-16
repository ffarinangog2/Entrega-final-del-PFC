package ec.edu.scli.usuarios.repository;

import ec.edu.scli.usuarios.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TecnicoRepository extends JpaRepository<Tecnico, UUID> {

    Optional<Tecnico> findByPerfilId(UUID perfilId);

    boolean existsByPerfilId(UUID perfilId);

    boolean existsByCodigoTecnico(String codigoTecnico);
}