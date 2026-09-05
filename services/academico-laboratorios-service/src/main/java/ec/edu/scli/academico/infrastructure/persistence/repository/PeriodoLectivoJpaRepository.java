package ec.edu.scli.academico.infrastructure.persistence.repository;

import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.infrastructure.persistence.entity.PeriodoLectivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface PeriodoLectivoJpaRepository
        extends JpaRepository<PeriodoLectivoEntity, UUID>,
        JpaSpecificationExecutor<PeriodoLectivoEntity> {

    Optional<PeriodoLectivoEntity> findFirstByEstadoOrderByFechaInicioDesc(EstadoPeriodo estado);

    @Query(value = """
            SELECT * FROM periodos_lectivos p
            WHERE p.fecha_inicio <= :fecha AND p.fecha_fin >= :fecha
            ORDER BY CASE WHEN p.ciclo_academico IS NOT NULL THEN 0 ELSE 1 END, p.fecha_inicio DESC
            limit 1
            """, nativeQuery = true)
    Optional<PeriodoLectivoEntity> buscarVigente(LocalDate fecha);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, UUID id);
}
