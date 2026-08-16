package ec.edu.scli.usuarios.service;

import ec.edu.scli.usuarios.dto.tecnico.TecnicoRequest;
import ec.edu.scli.usuarios.dto.tecnico.TecnicoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TecnicoService {

    TecnicoResponse crear(TecnicoRequest request);

    Page<TecnicoResponse> listar(Pageable pageable);

    TecnicoResponse obtenerPorId(UUID id);

    TecnicoResponse actualizar(UUID id, TecnicoRequest request);
}