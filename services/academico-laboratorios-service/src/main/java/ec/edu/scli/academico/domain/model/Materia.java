package ec.edu.scli.academico.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Materia, sin anotaciones JPA. */
public class Materia {

    private UUID id;
    private UUID carreraId;
    private String codigo;
    private String nombre;
    private Integer numeroHoras;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Materia() {
    }

    public static Materia nueva(UUID carreraId, String codigo, String nombre, Integer numeroHoras) {
        Materia materia = new Materia();
        materia.carreraId = carreraId;
        materia.codigo = codigo;
        materia.nombre = nombre;
        materia.numeroHoras = numeroHoras;
        materia.activo = true;
        return materia;
    }

    public void actualizarDatos(UUID carreraId, String codigo, String nombre, Integer numeroHoras) {
        this.carreraId = carreraId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.numeroHoras = numeroHoras;
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

    public UUID getCarreraId() {
        return carreraId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getNumeroHoras() {
        return numeroHoras;
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
