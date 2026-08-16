package ec.edu.uteq.scli.auth_service.security;

import ec.edu.uteq.scli.auth_service.entity.UsuarioAuth;
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

    public CustomUserDetails(UsuarioAuth usuario) {
        this.usuarioId = usuario.getId();
        this.perfilId = usuario.getPerfilId();
        this.username = usuario.getUsername();
        this.password = usuario.getPasswordHash();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
        this.cuentaBloqueada = Boolean.TRUE.equals(usuario.getCuentaBloqueada());

        this.authorities = new HashSet<>();

        usuario.getRoles().forEach(rol -> {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + rol.getCodigo()));

            rol.getPermisos().forEach(permiso -> authorities.add(
                    new SimpleGrantedAuthority(
                            permiso.getCodigo())));
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