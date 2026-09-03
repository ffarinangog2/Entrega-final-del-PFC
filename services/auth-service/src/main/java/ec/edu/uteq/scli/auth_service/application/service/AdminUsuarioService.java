package ec.edu.uteq.scli.auth_service.application.service;

import ec.edu.uteq.scli.auth_service.infrastructure.persistence.Rol;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.RolRepository;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuth;
import ec.edu.uteq.scli.auth_service.infrastructure.persistence.UsuarioAuthRepository;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioCreateRequest;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioResponse;
import ec.edu.uteq.scli.auth_service.presentation.dto.AdminUsuarioUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUsuarioService {
    private static final Set<String> ROLES_INSTITUCIONALES = Set.of(
            "ADMINISTRADOR", "ADMINISTRADOR_PISO", "COORDINADOR", "DOCENTE", "ESTUDIANTE");

    private final UsuarioAuthRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final PasswordPolicyValidator passwordPolicy;

    public AdminUsuarioService(UsuarioAuthRepository usuarios, RolRepository roles,
            PasswordEncoder encoder, PasswordPolicyValidator passwordPolicy) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponse> listar() {
        return usuarios.findAll().stream().map(this::response).toList();
    }

    @Transactional
    public AdminUsuarioResponse crear(AdminUsuarioCreateRequest request) {
        if (usuarios.findByPerfilId(request.perfilId()).isPresent()) {
            throw new IllegalArgumentException("El perfil ya tiene credenciales institucionales.");
        }
        validarUnicos(request.username(), request.email(), null);
        passwordPolicy.validate(request.passwordInicial());
        OffsetDateTime ahora = OffsetDateTime.now();
        UsuarioAuth usuario = new UsuarioAuth();
        usuario.setId(UUID.randomUUID());
        usuario.setPerfilId(request.perfilId());
        usuario.setUsername(request.username().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setPasswordHash(encoder.encode(request.passwordInicial()));
        usuario.setActivo(true);
        usuario.setCuentaBloqueada(false);
        usuario.setIntentosFallidos(0);
        usuario.setPasswordActualizadoEn(ahora);
        usuario.setCreadoEn(ahora);
        usuario.setActualizadoEn(ahora);
        usuario.getRoles().add(rol(request.rol()));
        return response(usuarios.save(usuario));
    }

    @Transactional
    public AdminUsuarioResponse actualizar(UUID id, AdminUsuarioUpdateRequest request) {
        UsuarioAuth usuario = usuarios.findWithRolesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario institucional no encontrado."));
        validarUnicos(request.username(), request.email(), id);
        usuario.setUsername(request.username().trim());
        usuario.setEmail(request.email().trim().toLowerCase());
        usuario.setActivo(request.activo());
        usuario.getRoles().clear();
        usuario.getRoles().add(rol(request.rol()));
        usuario.setActualizadoEn(OffsetDateTime.now());
        return response(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public AdminUsuarioResponse obtener(UUID id) {
        return response(usuarios.findWithRolesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario institucional no encontrado.")));
    }

    @Transactional
    public void eliminarCredencialCreada(UUID id, UUID perfilId) {
        UsuarioAuth usuario = usuarios.findWithRolesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario institucional no encontrado."));
        if (!usuario.getPerfilId().equals(perfilId)) {
            throw new IllegalArgumentException("La credencial no corresponde al perfil indicado.");
        }
        usuarios.delete(usuario);
    }

    private void validarUnicos(String username, String email, UUID actualId) {
        usuarios.findByUsernameIgnoreCase(username.trim()).filter(item -> !item.getId().equals(actualId))
                .ifPresent(item -> { throw new IllegalArgumentException("El nombre de usuario ya está registrado."); });
        usuarios.findByEmailIgnoreCase(email.trim()).filter(item -> !item.getId().equals(actualId))
                .ifPresent(item -> { throw new IllegalArgumentException("El correo ya está registrado."); });
    }

    private Rol rol(String codigo) {
        String normalizado = codigo.trim().toUpperCase();
        if (!ROLES_INSTITUCIONALES.contains(normalizado)) {
            throw new IllegalArgumentException("Rol institucional no válido.");
        }
        return roles.findByCodigoIgnoreCase(normalizado)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Rol institucional no disponible."));
    }

    private AdminUsuarioResponse response(UsuarioAuth usuario) {
        String rol = usuario.getRoles().stream().map(Rol::getCodigo).sorted().findFirst().orElse("");
        return new AdminUsuarioResponse(usuario.getId(), usuario.getPerfilId(), usuario.getUsername(),
                usuario.getEmail(), rol, Boolean.TRUE.equals(usuario.getActivo()));
    }
}
