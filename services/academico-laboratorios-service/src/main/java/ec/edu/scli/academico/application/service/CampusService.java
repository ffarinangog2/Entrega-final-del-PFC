package ec.edu.scli.academico.application.service;

import ec.edu.scli.academico.presentation.dto.campus.CampusRequest;
import ec.edu.scli.academico.presentation.dto.campus.CampusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CampusService {

    CampusResponse crear(CampusRequest request);

    Page<CampusResponse> listar(String codigo, String nombre, Boolean activo, Pageable pageable);

    CampusResponse obtenerPorId(UUID id);

    CampusResponse actualizar(UUID id, CampusRequest request);

    void eliminar(UUID id);
}
