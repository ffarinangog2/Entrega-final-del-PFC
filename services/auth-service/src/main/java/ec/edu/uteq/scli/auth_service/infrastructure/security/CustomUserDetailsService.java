package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import ec.edu.uteq.scli.auth_service.domain.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UserDetailsFactory userDetailsFactory;

    public CustomUserDetailsService(
            UsuarioRepository usuarioRepository,
            UserDetailsFactory userDetailsFactory) {
        this.usuarioRepository = usuarioRepository;
        this.userDetailsFactory = userDetailsFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .buscarConRolesPorIdentificador(identificador)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return userDetailsFactory.crear(usuario);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(UUID usuarioId) {

        Usuario usuario = usuarioRepository
                .buscarConRolesPorId(usuarioId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return userDetailsFactory.crear(usuario);
    }
}