package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.Rol;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioCreateRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {
    @Mock UsuarioAuthRepository usuarios;
    @Mock RolRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock PasswordPolicyValidator passwordPolicy;
    private AdminUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new AdminUsuarioService(usuarios, roles, encoder, passwordPolicy);
        lenient().when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaCredencialesConRolInstitucionalYPerfilSeleccionado() {
        UUID perfilId = UUID.randomUUID();
        Rol rol = rol("ADMINISTRADOR_PISO");
        when(roles.findByCodigoIgnoreCase("ADMINISTRADOR_PISO")).thenReturn(Optional.of(rol));
        when(encoder.encode("ClaveSegura1!")).thenReturn("hash");

        var creado = service.crear(new AdminUsuarioCreateRequest(
                perfilId, "admin.piso2", "piso2@scli.edu.ec", "ClaveSegura1!", "ADMINISTRADOR_PISO"));

        assertThat(creado.perfilId()).isEqualTo(perfilId);
        assertThat(creado.rol()).isEqualTo("ADMINISTRADOR_PISO");
        assertThat(creado.activo()).isTrue();
    }

    @Test
    void cambiaRolYEstadoEnLaMismaCuentaAuth() {
        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setId(UUID.randomUUID());
        usuario.setPerfilId(UUID.randomUUID());
        usuario.setUsername("coordinacion.demo");
        usuario.setEmail("coord@scli.edu.ec");
        usuario.setActivo(true);
        usuario.getRoles().add(rol("DOCENTE"));
        when(usuarios.findWithRolesById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(roles.findByCodigoIgnoreCase("COORDINADOR")).thenReturn(Optional.of(rol("COORDINADOR")));

        var actualizado = service.actualizar(usuario.getId(),
                new AdminUsuarioUpdateRequest("coordinacion.demo", "coord@scli.edu.ec", "COORDINADOR", false));

        assertThat(actualizado.id()).isEqualTo(usuario.getId());
        assertThat(actualizado.rol()).isEqualTo("COORDINADOR");
        assertThat(actualizado.activo()).isFalse();
    }

    @Test
    void compensacionEliminaSoloLaCredencialDelPerfilCreado() {
        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setId(UUID.randomUUID());
        usuario.setPerfilId(UUID.randomUUID());
        when(usuarios.findWithRolesById(usuario.getId())).thenReturn(Optional.of(usuario));

        service.eliminarCredencialCreada(usuario.getId(), usuario.getPerfilId());

        verify(usuarios).delete(usuario);
    }

    private Rol rol(String codigo) {
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        rol.setActivo(true);
        return rol;
    }
}
