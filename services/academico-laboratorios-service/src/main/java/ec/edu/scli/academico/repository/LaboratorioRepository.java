package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Laboratorio;
import ec.edu.scli.academico.enums.EstadoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface LaboratorioRepository
        extends JpaRepository<Laboratorio, UUID>,
        JpaSpecificationExecutor<Laboratorio> {

    List<Laboratorio> findByPisoId(UUID pisoId);

    List<Laboratorio> findByEstadoAndActivoTrue(EstadoLaboratorio estado);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    boolean existsByPisoId(UUID pisoId);
}
