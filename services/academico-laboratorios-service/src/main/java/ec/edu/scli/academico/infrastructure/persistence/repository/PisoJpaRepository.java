package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.infrastructure.persistence.entity.PisoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface PisoJpaRepository
        extends JpaRepository<PisoEntity, UUID>,
        JpaSpecificationExecutor<PisoEntity> {

    List<PisoEntity> findByBloqueId(UUID bloqueId);

    boolean existsByBloqueIdAndNumero(UUID bloqueId, Integer numero);

    boolean existsByBloqueIdAndNumeroAndIdNot(UUID bloqueId, Integer numero, UUID id);
}
