package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.PeriodoLectivo;
import ec.edu.scli.academico.enums.EstadoPeriodo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

public interface PeriodoLectivoRepositoryPort {

    PeriodoLectivo guardar(PeriodoLectivo periodoLectivo);

    Optional<PeriodoLectivo> buscarPorId(UUID id);

    Page<PeriodoLectivo> buscar(String codigo, Pageable pageable);

    Optional<PeriodoLectivo> buscarActualPorEstado(EstadoPeriodo estado);

    Optional<PeriodoLectivo> buscarVigente(LocalDate fecha);

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);

    boolean existePorId(UUID id);
}
