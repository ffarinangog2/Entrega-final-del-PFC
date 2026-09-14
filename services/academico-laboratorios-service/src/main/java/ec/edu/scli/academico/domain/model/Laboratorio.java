package ec.edu.scli.academico.domain.model;

import ec.edu.scli.academico.enums.EstadoLaboratorio;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Laboratorio, sin anotaciones JPA. */
public class Laboratorio {

    private UUID id;
    private UUID pisoId;
    private String codigo;
    private String nombre;
    private Integer capacidad;
    private String descripcion;
    private EstadoLaboratorio estado;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Laboratorio() {
    }

    public static Laboratorio nuevo(UUID pisoId, String codigo, String nombre, Integer capacidad, String descripcion) {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.pisoId = pisoId;
        laboratorio.codigo = codigo;
        laboratorio.nombre = nombre;
        laboratorio.capacidad = capacidad;
        laboratorio.descripcion = descripcion;
        laboratorio.estado = EstadoLaboratorio.DISPONIBLE;
        laboratorio.activo = true;
        return laboratorio;
    }

    public void actualizarDatos(UUID pisoId, String codigo, String nombre, Integer capacidad, String descripcion) {
        this.pisoId = pisoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.descripcion = descripcion;
    }

    /**
     * Regla de negocio: al pasar a INACTIVO también se da de baja lógica;
     * al pasar a cualquier otro estado, se reactiva si estaba dado de baja.
     */
    public void cambiarEstado(EstadoLaboratorio nuevoEstado) {
        this.estado = nuevoEstado;

        if (nuevoEstado == EstadoLaboratorio.INACTIVO) {
            this.activo = false;
        } else if (!this.activo) {
            this.activo = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPisoId() {
        return pisoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoLaboratorio getEstado() {
        return estado;
    }

    public void setEstado(EstadoLaboratorio estado) {
        this.estado = estado;
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
