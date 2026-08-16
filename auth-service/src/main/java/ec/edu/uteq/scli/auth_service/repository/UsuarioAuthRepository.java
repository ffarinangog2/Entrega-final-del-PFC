package ec.edu.uteq.scli.auth_service.repository;

import ec.edu.uteq.scli.auth_service.entity.UsuarioAuth;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioAuthRepository
                extends JpaRepository<UsuarioAuth, UUID> {

        Optional<UsuarioAuth> findByUsernameIgnoreCase(String username);

        Optional<UsuarioAuth> findByEmailIgnoreCase(String email);

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