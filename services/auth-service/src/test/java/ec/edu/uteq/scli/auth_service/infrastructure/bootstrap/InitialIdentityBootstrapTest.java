package ec.edu.uteq.scli.auth_service.infrastructure.bootstrap;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.Rol;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialIdentityBootstrapTest {
    @Test
    void createsPersistentAccountsWithConfiguredEncoderAndOneFunctionalRole() throws Exception {
        UsuarioAuthRepository users = mock(UsuarioAuthRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Rol role = new Rol(); role.setActivo(true);
        when(roles.findByCodigoIgnoreCase(any())).thenReturn(Optional.of(role));
        when(encoder.encode("configured-secret")).thenReturn("$2a$12$hash");

        var bootstrap = new InitialIdentityBootstrap(users, roles, encoder, "configured-secret");
        bootstrap.run(new DefaultApplicationArguments());

        verify(users, times(228)).save(argThat(user -> user.getRoles().size() == 1
                && user.getPasswordHash().startsWith("$2a$")));
        verify(encoder, times(228)).encode("configured-secret");

        clearInvocations(users, encoder);
        when(users.existsByUsernameIgnoreCase(any())).thenReturn(true);
        bootstrap.run(new DefaultApplicationArguments());
        verify(users, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void refusesEnabledBootstrapWithoutConfiguredPassword() {
        var bootstrap = new InitialIdentityBootstrap(mock(UsuarioAuthRepository.class),
                mock(RolRepository.class), mock(PasswordEncoder.class), "");
        assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments()));
    }
}
