package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface EquipoRepository
        extends JpaRepository<Equipo, UUID>,
        JpaSpecificationExecutor<Equipo> {

    List<Equipo> findByLaboratorioId(UUID laboratorioId);

    boolean existsByCodigoInventario(String codigoInventario);

    boolean existsByCodigoInventarioAndIdNot(String codigoInventario, UUID id);

    boolean existsByNumeroSerie(String numeroSerie);

    boolean existsByNumeroSerieAndIdNot(String numeroSerie, UUID id);

    boolean existsByTipoEquipoId(UUID tipoEquipoId);

    boolean existsByLaboratorioId(UUID laboratorioId);
}
