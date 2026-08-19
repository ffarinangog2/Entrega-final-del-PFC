package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Factory Method para crear el objeto de autenticación
 * utilizado por Spring Security.
 */
@Component
public class DefaultUserDetailsFactory implements UserDetailsFactory {

    @Override
    public CustomUserDetails crear(Usuario usuario) {
        return new CustomUserDetails(usuario);
    }
}