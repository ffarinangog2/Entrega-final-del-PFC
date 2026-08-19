package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;

public interface UserDetailsFactory {

    CustomUserDetails crear(Usuario usuario);
}