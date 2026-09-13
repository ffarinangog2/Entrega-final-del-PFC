package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import ec.edu.uteq.scli.auth_service.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UserDetailsFactory userDetailsFactory;

    @Mock
    private CustomUserDetails userDetails;

    private CustomUserDetailsService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(usuarioRepository, userDetailsFactory);
        usuario = new Usuario(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "admin",
                "admin@scli.local",
                "hash",
                true,
                false,
                Set.of());
    }

    @Test
    void cargaPorUsername() {
        when(usuarioRepository.buscarConRolesPorIdentificador("admin"))
                .thenReturn(Optional.of(usuario));
        when(userDetailsFactory.crear(usuario)).thenReturn(userDetails);

        assertSame(userDetails, service.loadUserByUsername("admin"));

        verify(userDetailsFactory).crear(usuario);
    }

    @Test
        void cargaPorIdentificadorAlternativo() {
                when(usuarioRepository.buscarConRolesPorIdentificador("admin@scli.local"))
                .thenReturn(Optional.of(usuario));
        when(userDetailsFactory.crear(usuario)).thenReturn(userDetails);

        assertSame(userDetails, service.loadUserByUsername("admin@scli.local"));
    }

    @Test
    void usernameInexistenteLanzaExcepcion() {
        when(usuarioRepository.buscarConRolesPorIdentificador("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing"));
    }

    @Test
    void cargaPorId() {
        UUID usuarioId = usuario.id();
        when(usuarioRepository.buscarConRolesPorId(usuarioId))
                .thenReturn(Optional.of(usuario));
        when(userDetailsFactory.crear(usuario)).thenReturn(userDetails);

        assertSame(userDetails, service.loadUserById(usuarioId));
    }

    @Test
    void idInexistenteLanzaExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.buscarConRolesPorId(usuarioId))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserById(usuarioId));
    }
}
