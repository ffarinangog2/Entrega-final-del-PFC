package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Carrera, sin anotaciones JPA. */
public class Carrera {

    private UUID id;
    private UUID facultadId;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Carrera() {
    }

    public static Carrera nueva(UUID facultadId, String codigo, String nombre, String descripcion) {
        Carrera carrera = new Carrera();
        carrera.facultadId = facultadId;
        carrera.codigo = codigo;
        carrera.nombre = nombre;
        carrera.descripcion = descripcion;
        carrera.activo = true;
        return carrera;
    }

    public void actualizarDatos(UUID facultadId, String codigo, String nombre, String descripcion) {
        this.facultadId = facultadId;
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

    public UUID getFacultadId() {
        return facultadId;
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
