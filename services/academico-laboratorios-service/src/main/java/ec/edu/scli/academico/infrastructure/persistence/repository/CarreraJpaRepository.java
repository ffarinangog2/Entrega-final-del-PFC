package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.CarreraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface CarreraJpaRepository
        extends JpaRepository<CarreraEntity, UUID>,
        JpaSpecificationExecutor<CarreraEntity> {

    List<CarreraEntity> findByFacultadId(UUID facultadId);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByFacultadId(UUID facultadId);
}
