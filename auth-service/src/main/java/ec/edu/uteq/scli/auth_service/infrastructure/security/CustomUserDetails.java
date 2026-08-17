package ec.edu.uteq.scli.auth_service.infrastructure.security;

import ec.edu.uteq.scli.auth_service.domain.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID usuarioId;
    private final UUID perfilId;
    private final String username;
    private final String password;
    private final boolean activo;
    private final boolean cuentaBloqueada;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.usuarioId = usuario.id();
        this.perfilId = usuario.perfilId();
        this.username = usuario.username();
        this.password = usuario.passwordHash();
        this.activo = usuario.activo();
        this.cuentaBloqueada = usuario.cuentaBloqueada();

        this.authorities = new HashSet<>();

        usuario.roles().forEach(rol -> {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + rol.codigo()));

            rol.permisos().forEach(permiso -> authorities.add(
                    new SimpleGrantedAuthority(
                            permiso.codigo())));
        });
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getPerfilId() {
        return perfilId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !cuentaBloqueada;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
