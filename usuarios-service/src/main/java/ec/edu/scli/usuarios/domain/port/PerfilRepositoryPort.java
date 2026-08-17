package ec.edu.scli.usuarios.domain.port;

import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;
import ec.edu.scli.usuarios.domain.pagination.PageCriteria;
import ec.edu.scli.usuarios.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepositoryPort {

    Perfil save(Perfil perfil);

    Optional<Perfil> findById(UUID id);

    Optional<Perfil> findByIdentificacion(String identificacion);

    Optional<Perfil> findByEmailInstitucional(String emailInstitucional);

    boolean existsByIdentificacion(String identificacion);

    boolean existsByEmailInstitucional(String emailInstitucional);

    PageResult<Perfil> findAll(
            String identificacion,
            String nombre,
            String email,
            TipoPerfil tipoPerfil,
            Boolean activo,
            PageCriteria pageCriteria
    );
}
