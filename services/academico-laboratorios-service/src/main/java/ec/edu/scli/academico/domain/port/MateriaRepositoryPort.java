package ec.edu.scli.academico.domain.port;

import ec.edu.scli.academico.domain.model.Materia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MateriaRepositoryPort {

    Materia guardar(Materia materia);

    Optional<Materia> buscarPorId(UUID id);

    Page<Materia> buscar(UUID carreraId, String codigo, String nombre, Boolean activo, Pageable pageable);

    List<Materia> buscarPorCarrera(UUID carreraId);

    boolean existeCodigo(String codigo);

    boolean existeCodigoParaOtroId(String codigo, UUID id);

    boolean existePorId(UUID id);
}
