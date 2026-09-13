package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.Administrador;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdministradorRepositoryPort {

    Administrador save(Administrador administrador);
    PageResult<Administrador> findAll(PageCriteria pageCriteria);
    List<Administrador> findAll();
    Optional<Administrador> findById(UUID id);
    Optional<Administrador> findByPerfilId(UUID perfilId);
    boolean existsByPerfilId(UUID perfilId);
    boolean existsByCodigoAdministrador(String codigoAdministrador);
}
