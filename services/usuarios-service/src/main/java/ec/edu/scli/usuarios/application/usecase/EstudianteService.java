package ec.edu.scli.usuarios.application.usecase;

import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteRequest;
import ec.edu.scli.usuarios.presentation.dto.estudiante.EstudianteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EstudianteService {

    EstudianteResponse crear(EstudianteRequest request);

    Page<EstudianteResponse> listar(Pageable pageable);

    EstudianteResponse obtenerPorId(UUID id);
    EstudianteResponse obtenerPorPerfilId(UUID perfilId);

    EstudianteResponse actualizar(UUID id, EstudianteRequest request);
}
