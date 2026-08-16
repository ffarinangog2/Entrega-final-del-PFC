package ec.edu.scli.academico.repository;

import ec.edu.scli.academico.entity.PeriodoLectivo;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PeriodoLectivoRepository
        extends JpaRepository<PeriodoLectivo, UUID>,
        JpaSpecificationExecutor<PeriodoLectivo> {

    Optional<PeriodoLectivo> findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo estado);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
