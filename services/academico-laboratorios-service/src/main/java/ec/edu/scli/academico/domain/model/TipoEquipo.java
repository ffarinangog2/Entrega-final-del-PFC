package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de TipoEquipo, sin anotaciones JPA. */
public class TipoEquipo {

    private UUID id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public TipoEquipo() {
    }

    public static TipoEquipo nuevo(String codigo, String nombre, String descripcion) {
        TipoEquipo tipoEquipo = new TipoEquipo();
        tipoEquipo.codigo = codigo;
        tipoEquipo.nombre = nombre;
        tipoEquipo.descripcion = descripcion;
        tipoEquipo.activo = true;
        return tipoEquipo;
    }

    public void actualizarDatos(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
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

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
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
