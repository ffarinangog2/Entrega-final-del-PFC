package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Piso, sin anotaciones JPA. */
public class Piso {

    private UUID id;
    private UUID bloqueId;
    private Integer numero;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Piso() {
    }

    public static Piso nuevo(UUID bloqueId, Integer numero, String descripcion) {
        Piso piso = new Piso();
        piso.bloqueId = bloqueId;
        piso.numero = numero;
        piso.descripcion = descripcion;
        piso.activo = true;
        return piso;
    }

    public void actualizarDatos(UUID bloqueId, Integer numero, String descripcion) {
        this.bloqueId = bloqueId;
        this.numero = numero;
        this.descripcion = descripcion;
    }

    public void desactivar() {
        this.activo = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBloqueId() {
        return bloqueId;
    }

    public Integer getNumero() {
        return numero;
    }

    public String getDescripcion() {
        return descripcion;
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
