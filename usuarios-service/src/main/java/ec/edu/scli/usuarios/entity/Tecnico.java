package ec.edu.scli.usuarios.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tecnicos")
public class Tecnico {

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
            name = "codigo_tecnico",
            nullable = false,
            unique = true,
            length = 30
    )
    private String codigoTecnico;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    public Tecnico() {
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

    public String getCodigoTecnico() {
        return codigoTecnico;
    }

    public void setCodigoTecnico(String codigoTecnico) {
        this.codigoTecnico = codigoTecnico;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
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