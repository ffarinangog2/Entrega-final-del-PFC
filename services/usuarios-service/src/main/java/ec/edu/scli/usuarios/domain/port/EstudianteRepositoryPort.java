package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.Estudiante;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepositoryPort {

    Estudiante save(Estudiante estudiante);
    PageResult<Estudiante> findAll(PageCriteria pageCriteria);
    List<Estudiante> findAll();
    Optional<Estudiante> findById(UUID id);
    Optional<Estudiante> findByPerfilId(UUID perfilId);
    boolean existsByPerfilId(UUID perfilId);
    boolean existsByMatricula(String matricula);
}
