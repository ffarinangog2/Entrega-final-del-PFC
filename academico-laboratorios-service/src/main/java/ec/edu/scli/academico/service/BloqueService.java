package ec.edu.scli.academico.service;

import ec.edu.scli.academico.dto.bloque.BloqueRequest;
import ec.edu.scli.academico.dto.bloque.BloqueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BloqueService {

    BloqueResponse crear(BloqueRequest request);

    Page<BloqueResponse> listar(UUID campusId, String nombre, Boolean activo, Pageable pageable);

    List<BloqueResponse> listarPorCampus(UUID campusId);

    BloqueResponse obtenerPorId(UUID id);

    BloqueResponse actualizar(UUID id, BloqueRequest request);

    void eliminar(UUID id);
}
