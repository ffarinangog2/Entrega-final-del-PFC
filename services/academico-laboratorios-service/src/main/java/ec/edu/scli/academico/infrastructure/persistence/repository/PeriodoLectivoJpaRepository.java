package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PeriodoLectivoJpaRepository
        extends JpaRepository<PeriodoLectivoEntity, UUID>,
        JpaSpecificationExecutor<PeriodoLectivoEntity> {

    Optional<PeriodoLectivoEntity> findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo estado);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
