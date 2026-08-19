package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.piso.PisoRequest;
import ec.edu.scli.academico.dto.piso.PisoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PisoService {

    PisoResponse crear(PisoRequest request);

    Page<PisoResponse> listar(UUID bloqueId, Boolean activo, Pageable pageable);

    List<PisoResponse> listarPorBloque(UUID bloqueId);

    PisoResponse obtenerPorId(UUID id);

    PisoResponse actualizar(UUID id, PisoRequest request);

    void eliminar(UUID id);
}
