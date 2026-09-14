package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.Docente;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocenteRepositoryPort {

    Docente save(Docente docente);
    PageResult<Docente> findAll(PageCriteria pageCriteria);
    List<Docente> findAll();
    Optional<Docente> findById(UUID id);
    Optional<Docente> findByPerfilId(UUID perfilId);
    boolean existsByPerfilId(UUID perfilId);
    boolean existsByCodigoDocente(String codigoDocente);
}
