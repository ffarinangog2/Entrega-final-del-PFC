package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface CarreraRepository
        extends JpaRepository<Carrera, UUID>,
        JpaSpecificationExecutor<Carrera> {

    List<Carrera> findByFacultadId(UUID facultadId);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByFacultadId(UUID facultadId);
}
