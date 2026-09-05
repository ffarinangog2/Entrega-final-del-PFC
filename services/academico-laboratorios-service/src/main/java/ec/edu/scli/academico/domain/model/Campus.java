package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modelo de dominio de Campus. No conoce JPA ni ningún detalle de
 * persistencia: es un objeto plano que representa las reglas e
 * invariantes propias del negocio.
 */
public class Campus {

    private UUID id;
    private String codigo;
    private String nombre;
    private String direccion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Campus() {
    }

    public static Campus nuevo(String codigo, String nombre, String direccion) {
        Campus campus = new Campus();
        campus.codigo = codigo;
        campus.nombre = nombre;
        campus.direccion = direccion;
        campus.activo = true;
        return campus;
    }

    /** Aplica los datos editables de una edición, sin tocar id ni estado. */
    public void actualizarDatos(String codigo, String nombre, String direccion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
    }

    /** Baja lógica: regla de negocio propia del dominio, no un simple setter externo. */
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

    public String getDireccion() {
        return direccion;
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
