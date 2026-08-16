package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface MateriaRepository
        extends JpaRepository<Materia, UUID>,
        JpaSpecificationExecutor<Materia> {

    List<Materia> findByCarreraId(UUID carreraId);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByCarreraId(UUID carreraId);
}
