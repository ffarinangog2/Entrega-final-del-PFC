package ec.edu.scli.academico.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pisos")
public class Piso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bloque_id", nullable = false)
    private UUID bloqueId;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @PrePersist
    protected void alPersistir() {
        OffsetDateTime ahora = OffsetDateTime.now();
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        this.actualizadoEn = OffsetDateTime.now();
    }

    // ---------- Getters y Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloqueId() {
        return bloqueId;
    }

    public void setBloqueId(UUID bloqueId) {
        this.bloqueId = bloqueId;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
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
