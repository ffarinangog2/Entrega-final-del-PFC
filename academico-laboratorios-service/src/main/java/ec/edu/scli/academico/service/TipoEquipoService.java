package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoRequest;
import ec.edu.scli.academico.dto.tipoequipo.TipoEquipoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TipoEquipoService {

    TipoEquipoResponse crear(TipoEquipoRequest request);

    Page<TipoEquipoResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable);

    TipoEquipoResponse obtenerPorId(UUID id);

    TipoEquipoResponse actualizar(UUID id, TipoEquipoRequest request);

    void eliminar(UUID id);
}
