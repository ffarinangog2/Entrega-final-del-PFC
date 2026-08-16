package ec.edu.uteq.scli.auth_service.security;

import ec.edu.uteq.scli.auth_service.entity.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.repository.UsuarioAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioAuthRepository usuarioAuthRepository;

    public CustomUserDetailsService(
            UsuarioAuthRepository usuarioAuthRepository) {
        this.usuarioAuthRepository = usuarioAuthRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador)
            throws UsernameNotFoundException {

        UsuarioAuth usuario = usuarioAuthRepository
                .findWithRolesByUsernameIgnoreCase(identificador)
                .orElseGet(() -> usuarioAuthRepository
                        .findWithRolesByEmailIgnoreCase(identificador)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "Usuario no encontrado")));

        return new CustomUserDetails(usuario);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(UUID usuarioId) {

        UsuarioAuth usuario = usuarioAuthRepository
                .findWithRolesById(usuarioId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado"));

        return new CustomUserDetails(usuario);
    }
}