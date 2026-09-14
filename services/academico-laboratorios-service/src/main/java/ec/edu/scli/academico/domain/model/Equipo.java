package ec.edu.scli.academico.domain.model;

import ec.edu.scli.academico.enums.EstadoEquipo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de Equipo, sin anotaciones JPA. */
public class Equipo {

    private UUID id;
    private UUID laboratorioId;
    private UUID tipoEquipoId;
    private String codigoInventario;
    private String numeroSerie;
    private String marca;
    private String modelo;
    private String procesador;
    private String memoriaRam;
    private String almacenamiento;
    private String direccionIp;
    private String direccionMac;
    private EstadoEquipo estado;
    private String observacion;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public Equipo() {
    }

    public static Equipo nuevo(
            UUID laboratorioId,
            UUID tipoEquipoId,
            String codigoInventario,
            String numeroSerie,
            String marca,
            String modelo,
            String procesador,
            String memoriaRam,
            String almacenamiento,
            String direccionIp,
            String direccionMac,
            String observacion
    ) {
        Equipo equipo = new Equipo();
        equipo.aplicarDatos(
                laboratorioId, tipoEquipoId, codigoInventario, numeroSerie,
                marca, modelo, procesador, memoriaRam, almacenamiento,
                direccionIp, direccionMac, observacion
        );
        equipo.estado = EstadoEquipo.OPERATIVO;
        equipo.activo = true;
        return equipo;
    }

    public void aplicarDatos(
            UUID laboratorioId,
            UUID tipoEquipoId,
            String codigoInventario,
            String numeroSerie,
            String marca,
            String modelo,
            String procesador,
            String memoriaRam,
            String almacenamiento,
            String direccionIp,
            String direccionMac,
            String observacion
    ) {
        this.laboratorioId = laboratorioId;
        this.tipoEquipoId = tipoEquipoId;
        this.codigoInventario = codigoInventario;
        this.numeroSerie = numeroSerie;
        this.marca = marca;
        this.modelo = modelo;
        this.procesador = procesador;
        this.memoriaRam = memoriaRam;
        this.almacenamiento = almacenamiento;
        this.direccionIp = direccionIp;
        this.direccionMac = direccionMac;
        this.observacion = observacion;
    }

    /**
     * Regla de negocio: al pasar a FUERA_DE_SERVICIO también se da de baja
     * lógica; al pasar a cualquier otro estado, se reactiva si estaba dado
     * de baja.
     */
    public void cambiarEstado(EstadoEquipo nuevoEstado) {
        this.estado = nuevoEstado;

        if (nuevoEstado == EstadoEquipo.FUERA_DE_SERVICIO) {
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

    public UUID getLaboratorioId() {
        return laboratorioId;
    }

    public UUID getTipoEquipoId() {
        return tipoEquipoId;
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public String getProcesador() {
        return procesador;
    }

    public String getMemoriaRam() {
        return memoriaRam;
    }

    public String getAlmacenamiento() {
        return almacenamiento;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public String getDireccionMac() {
        return direccionMac;
    }

    public EstadoEquipo getEstado() {
        return estado;
    }

    public void setEstado(EstadoEquipo estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
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
