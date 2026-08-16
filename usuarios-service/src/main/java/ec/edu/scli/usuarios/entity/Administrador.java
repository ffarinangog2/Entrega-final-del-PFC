package ec.edu.scli.usuarios.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "administradores")
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "perfil_id",
            nullable = false,
            unique = true
    )
    private Perfil perfil;

    @Column(
            name = "codigo_administrador",
            nullable = false,
            unique = true,
            length = 30
    )
    private String codigoAdministrador;

    @Column(name = "cargo", length = 100)
    private String cargo;

    /*
     * pisoId pertenece al microservicio de laboratorios.
     * Se almacena como UUID externo, sin relación JPA
     * ni clave foránea hacia otra base de datos.
     */
    @Column(name = "piso_id")
    private UUID pisoId;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public Administrador() {
    }

    @PrePersist
    public void prePersist() {

        OffsetDateTime ahora = OffsetDateTime.now();

        creadoEn = ahora;
        actualizadoEn = ahora;

        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        actualizadoEn = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getCodigoAdministrador() {
        return codigoAdministrador;
    }

    public void setCodigoAdministrador(String codigoAdministrador) {
        this.codigoAdministrador = codigoAdministrador;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public UUID getPisoId() {
        return pisoId;
    }

    public void setPisoId(UUID pisoId) {
        this.pisoId = pisoId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(OffsetDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}