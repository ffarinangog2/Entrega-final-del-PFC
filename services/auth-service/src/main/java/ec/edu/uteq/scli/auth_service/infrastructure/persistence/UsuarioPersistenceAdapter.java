package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import ec.edu.uteq.scli.auth_service.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA del puerto de usuarios del dominio. */
@Repository
class UsuarioPersistenceAdapter implements UsuarioRepository {
    private final UsuarioAuthRepository repository;

    UsuarioPersistenceAdapter(UsuarioAuthRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarConRolesPorIdentificador(String identificador) {
        return repository.findWithRolesByUsernameIgnoreCase(identificador)
                .or(() -> repository.findWithRolesByEmailIgnoreCase(identificador))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarConRolesPorId(UUID id) {
        return repository.findWithRolesById(id).map(this::toDomain);
    }

    private Usuario toDomain(UsuarioAuth entity) {
        return new Usuario(entity.getId(), entity.getPerfilId(), entity.getUsername(), entity.getEmail(),
                entity.getPasswordHash(), Boolean.TRUE.equals(entity.getActivo()),
                Boolean.TRUE.equals(entity.getCuentaBloqueada()),
                entity.getRoles().stream().map(this::toDomain).collect(java.util.stream.Collectors.toSet()));
    }

    private ec.edu.uteq.scli.auth_service.domain.model.Rol toDomain(Rol entity) {
        return new ec.edu.uteq.scli.auth_service.domain.model.Rol(entity.getId(), entity.getCodigo(),
                entity.getNombre(), entity.getDescripcion(),
                Boolean.TRUE.equals(entity.getActivo()),
                entity.getPermisos().stream().map(this::toDomain).collect(java.util.stream.Collectors.toSet()));
    }

    private ec.edu.uteq.scli.auth_service.domain.model.Permiso toDomain(Permiso entity) {
        return new ec.edu.uteq.scli.auth_service.domain.model.Permiso(entity.getId(), entity.getCodigo(),
                entity.getNombre(), entity.getDescripcion(), Boolean.TRUE.equals(entity.getActivo()));
    }
}
