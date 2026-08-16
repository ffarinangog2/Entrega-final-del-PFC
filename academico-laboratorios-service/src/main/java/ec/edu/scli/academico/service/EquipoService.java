package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.equipo.EquipoRequest;
import ec.edu.scli.academico.dto.equipo.EquipoResponse;
import ec.edu.scli.academico.enums.EstadoEquipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EquipoService {

    EquipoResponse crear(EquipoRequest request);

    Page<EquipoResponse> listar(UUID laboratorioId, EstadoEquipo estado, Boolean activo, Pageable pageable);

    List<EquipoResponse> listarPorLaboratorio(UUID laboratorioId);

    EquipoResponse obtenerPorId(UUID id);

    EquipoResponse actualizar(UUID id, EquipoRequest request);

    EquipoResponse cambiarEstado(UUID id, EstadoEquipo estado);
}
