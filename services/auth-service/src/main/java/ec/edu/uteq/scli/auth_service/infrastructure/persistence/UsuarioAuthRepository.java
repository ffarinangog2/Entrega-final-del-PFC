package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioAuthRepository
                extends JpaRepository<UsuarioAuth, UUID> {

        Optional<UsuarioAuth> findByUsernameIgnoreCase(String username);

        Optional<UsuarioAuth> findByEmailIgnoreCase(String email);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<UsuarioAuth> findLockedByUsernameIgnoreCase(String username);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<UsuarioAuth> findLockedByEmailIgnoreCase(String email);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<UsuarioAuth> findLockedById(UUID id);

        Optional<UsuarioAuth> findByPerfilId(UUID perfilId);

        boolean existsByUsernameIgnoreCase(String username);

        boolean existsByEmailIgnoreCase(String email);

        @EntityGraph(attributePaths = {
                        "roles",
                        "roles.permisos"
        })
        Optional<UsuarioAuth> findWithRolesByUsernameIgnoreCase(
                        String username);

        @EntityGraph(attributePaths = {
                        "roles",
                        "roles.permisos"
        })
        Optional<UsuarioAuth> findWithRolesByEmailIgnoreCase(
                        String email);

        @EntityGraph(attributePaths = {
                        "roles",
                        "roles.permisos"
        })
        Optional<UsuarioAuth> findWithRolesById(UUID id);
}
