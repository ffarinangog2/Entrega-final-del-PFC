package ec.edu.uteq.scli.auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuarios_auth")
public class UsuarioAuth {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "perfil_id", nullable = false)
    private UUID perfilId;

    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "cuenta_bloqueada", nullable = false)
    private Boolean cuentaBloqueada;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "ultimo_login")
    private OffsetDateTime ultimoLogin;

    @Column(name = "password_actualizado_en")
    private OffsetDateTime passwordActualizadoEn;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();
}