package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Carrera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarreraRepositoryPort {

    Carrera guardar(Carrera carrera);

    Optional<Carrera> buscarPorId(UUID id);

    Page<Carrera> buscar(UUID facultadId, String codigo, String nombre, Boolean activo, Pageable pageable);

    List<Carrera> buscarPorFacultad(UUID facultadId);

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);

    boolean existePorFacultad(UUID facultadId);
}
