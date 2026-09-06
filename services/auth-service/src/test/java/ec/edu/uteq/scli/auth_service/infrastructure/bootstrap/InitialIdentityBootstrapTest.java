package ec.edu.uteq.scli.auth_service.infrastructure.bootstrap;

import ec.edu.uteq.scli.auth_service.application.service.RefreshSessionService;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.Rol;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialIdentityBootstrapTest {
    @Test
    void createsPersistentAccountsWithConfiguredEncoderAndOneFunctionalRole() throws Exception {
        UsuarioAuthRepository users = mock(UsuarioAuthRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        RefreshSessionService refreshSessions = mock(RefreshSessionService.class);
        Rol role = new Rol(); role.setActivo(true);
        when(roles.findByCodigoIgnoreCase(any())).thenReturn(Optional.of(role));
        when(encoder.encode("configured-secret")).thenReturn("$2a$12$hash");

        var bootstrap = new InitialIdentityBootstrap(users, roles, encoder, refreshSessions, "configured-secret");
        bootstrap.run(new DefaultApplicationArguments());

        verify(users, times(226)).save(argThat(user -> user.getRoles().size() == 1
                && user.getPasswordHash().startsWith("$2a$")));
        verify(encoder, times(226)).encode("configured-secret");

        clearInvocations(users, encoder);
        when(users.existsByUsernameIgnoreCase(any())).thenReturn(true);
        bootstrap.run(new DefaultApplicationArguments());
        verify(users, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void refusesEnabledBootstrapWithoutConfiguredPassword() {
        var bootstrap = new InitialIdentityBootstrap(mock(UsuarioAuthRepository.class),
                mock(RolRepository.class), mock(PasswordEncoder.class), mock(RefreshSessionService.class), "");
        assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments()));
    }


    @Test
    void preservesCanonicalAdminAndDisablesLegacyGlobalsIdempotently() throws Exception {
        UsuarioAuthRepository users = mock(UsuarioAuthRepository.class);
        RolRepository roles = mock(RolRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        RefreshSessionService refreshSessions = mock(RefreshSessionService.class);
        Rol administratorRole = new Rol();
        administratorRole.setCodigo("ADMINISTRADOR");
        UsuarioAuth admin = user("a0000000-0000-0000-0000-000000000001", "admin", true);
        UsuarioAuth legacyOne = user("11000000-0000-0000-0000-000000000001",
                "administrador.facultad01", true);
        UsuarioAuth legacyTwo = user("11000000-0000-0000-0000-000000000002",
                "administrador.facultad02", true);
        admin.getRoles().add(administratorRole);
        legacyOne.getRoles().add(administratorRole);
        legacyTwo.getRoles().add(administratorRole);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(users.findByUsernameIgnoreCase("administrador.facultad01")).thenReturn(Optional.of(legacyOne));
        when(users.findByUsernameIgnoreCase("administrador.facultad02")).thenReturn(Optional.of(legacyTwo));
        when(users.existsByUsernameIgnoreCase(any())).thenReturn(true);

        var bootstrap = new InitialIdentityBootstrap(users, roles, encoder, refreshSessions, "configured-secret");
        bootstrap.run(new DefaultApplicationArguments());

        assertTrue(admin.getActivo());
        assertTrue(admin.getRoles().stream().anyMatch(role -> "ADMINISTRADOR".equals(role.getCodigo())));
        assertFalse(legacyOne.getActivo());
        assertFalse(legacyTwo.getActivo());
        assertTrue(legacyOne.getRoles().contains(administratorRole));
        assertTrue(legacyTwo.getRoles().contains(administratorRole));
        verify(users).save(legacyOne);
        verify(users).save(legacyTwo);
        verify(users, never()).save(admin);
        verify(refreshSessions).revocarActivasPorUsuario(legacyOne.getId());
        verify(refreshSessions).revocarActivasPorUsuario(legacyTwo.getId());

        clearInvocations(users, refreshSessions);
        bootstrap.run(new DefaultApplicationArguments());

        verify(users, never()).save(any());
        verify(refreshSessions).revocarActivasPorUsuario(legacyOne.getId());
        verify(refreshSessions).revocarActivasPorUsuario(legacyTwo.getId());
    }

    private UsuarioAuth user(String id, String username, boolean active) {
        UsuarioAuth user = new UsuarioAuth();
        user.setId(UUID.fromString(id));
        user.setUsername(username);
        user.setActivo(active);
        return user;
    }
}
