package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.FacultadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FacultadJpaRepository
        extends JpaRepository<FacultadEntity, UUID>,
        JpaSpecificationExecutor<FacultadEntity> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
