package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Facultad, sin anotaciones JPA. */
public class Facultad {

    private UUID id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Facultad() {
    }

    public static Facultad nueva(String codigo, String nombre, String descripcion) {
        Facultad facultad = new Facultad();
        facultad.codigo = codigo;
        facultad.nombre = nombre;
        facultad.descripcion = descripcion;
        facultad.activo = true;
        return facultad;
    }

    public void actualizarDatos(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /** Regla de negocio: cambia el estado activo/inactivo de la facultad. */
    public void cambiarEstado(boolean activo) {
        this.activo = activo;
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
