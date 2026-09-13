package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Bloque, sin anotaciones JPA. */
public class Bloque {

    private UUID id;
    private UUID campusId;
    private String codigo;
    private String nombre;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Bloque() {
    }

    public static Bloque nuevo(UUID campusId, String codigo, String nombre) {
        Bloque bloque = new Bloque();
        bloque.campusId = campusId;
        bloque.codigo = codigo;
        bloque.nombre = nombre;
        bloque.activo = true;
        return bloque;
    }

    public void actualizarDatos(UUID campusId, String codigo, String nombre) {
        this.campusId = campusId;
        this.codigo = codigo;
        this.nombre = nombre;
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

    public UUID getCampusId() {
        return campusId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
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
