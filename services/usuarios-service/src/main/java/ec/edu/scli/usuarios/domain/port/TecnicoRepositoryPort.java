package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.Tecnico;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TecnicoRepositoryPort {

    Tecnico save(Tecnico tecnico);
    PageResult<Tecnico> findAll(PageCriteria pageCriteria);
    List<Tecnico> findAll();
    Optional<Tecnico> findById(UUID id);
    Optional<Tecnico> findByPerfilId(UUID perfilId);
    boolean existsByPerfilId(UUID perfilId);
    boolean existsByCodigoTecnico(String codigoTecnico);
}
