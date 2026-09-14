package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.MateriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MateriaJpaRepository
        extends JpaRepository<MateriaEntity, UUID>,
        JpaSpecificationExecutor<MateriaEntity> {

    List<MateriaEntity> findByCarreraId(UUID carreraId);

    boolean existsByCodigo(String codigo);

    Optional<MateriaEntity> findByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByCarreraId(UUID carreraId);
}
