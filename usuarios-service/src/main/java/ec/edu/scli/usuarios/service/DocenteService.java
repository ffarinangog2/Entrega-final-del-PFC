package ec.edu.scli.usuarios.service;

import ec.edu.scli.usuarios.dto.docente.DocenteRequest;
import ec.edu.scli.usuarios.dto.docente.DocenteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocenteService {

    DocenteResponse crear(DocenteRequest request);

    Page<DocenteResponse> listar(Pageable pageable);

    DocenteResponse obtenerPorId(UUID id);

    DocenteResponse actualizar(UUID id, DocenteRequest request);

    DocenteResponse obtenerPorPerfilId(UUID perfilId);
}