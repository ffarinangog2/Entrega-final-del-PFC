package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Piso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface PisoRepository
        extends JpaRepository<Piso, UUID>,
        JpaSpecificationExecutor<Piso> {

    List<Piso> findByBloqueId(UUID bloqueId);

    boolean existsByBloqueIdAndNumero(UUID bloqueId, Integer numero);

    boolean existsByBloqueIdAndNumeroAndIdNot(UUID bloqueId, Integer numero, UUID id);
}
