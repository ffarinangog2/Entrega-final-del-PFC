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
    void createsTenPersistentAccountsWithBcryptEncoderAndOneFunctionalRole() throws Exception {
        UsuarioAuthRepository users = mock(UsuarioAuthRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Rol role = new Rol(); role.setActivo(true);
        when(roles.findByCodigoIgnoreCase(any())).thenReturn(Optional.of(role));
        when(encoder.encode("configured-secret")).thenReturn("$2a$12$hash");

        new InitialIdentityBootstrap(users, roles, encoder, "configured-secret")
                .run(new DefaultApplicationArguments());

        verify(users, times(10)).save(argThat(user -> user.getRoles().size() == 1
                && user.getPasswordHash().startsWith("$2a$")));
        verify(encoder, times(10)).encode("configured-secret");
    }

    @Test
    void refusesEnabledBootstrapWithoutConfiguredPassword() {
        var bootstrap = new InitialIdentityBootstrap(mock(UsuarioAuthRepository.class),
                mock(RolRepository.class), mock(PasswordEncoder.class), "");
        assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments()));
    }
}
