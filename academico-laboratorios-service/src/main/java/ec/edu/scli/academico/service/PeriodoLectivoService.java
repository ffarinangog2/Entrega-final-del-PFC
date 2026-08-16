package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.internal.ExisteResponse;
import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoRequest;
import ec.edu.scli.academico.dto.periodolectivo.PeriodoLectivoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PeriodoLectivoService {

    PeriodoLectivoResponse crear(PeriodoLectivoRequest request);

    Page<PeriodoLectivoResponse> listar(String codigo, Pageable pageable);

    PeriodoLectivoResponse obtenerPorId(UUID id);

    PeriodoLectivoResponse obtenerActual();

    PeriodoLectivoResponse actualizar(UUID id, PeriodoLectivoRequest request);

    ExisteResponse verificarExistencia(UUID id);
}
