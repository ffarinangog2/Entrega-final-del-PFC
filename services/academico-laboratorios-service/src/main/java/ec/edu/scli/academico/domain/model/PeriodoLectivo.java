package ec.edu.scli.academico.domain.model;

import ec.edu.scli.academico.enums.EstadoPeriodo;
import ec.edu.scli.academico.exception.BusinessRuleException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de PeriodoLectivo, sin anotaciones JPA. */
public class PeriodoLectivo {

    private UUID id;
    private String codigo;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoPeriodo estado;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public PeriodoLectivo() {
    }

    public static PeriodoLectivo nuevo(
            String codigo,
            String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoPeriodo estado
    ) {
        PeriodoLectivo periodo = new PeriodoLectivo();
        periodo.aplicarFechas(fechaInicio, fechaFin);
        periodo.codigo = codigo;
        periodo.nombre = nombre;
        periodo.estado = estado != null ? estado : EstadoPeriodo.PLANIFICADO;
        return periodo;
    }

    public void actualizarDatos(String codigo, String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        aplicarFechas(fechaInicio, fechaFin);
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public void cambiarEstado(EstadoPeriodo nuevoEstado) {
        if (nuevoEstado != null) {
            this.estado = nuevoEstado;
        }
    }

    /** Regla de negocio: la fecha de fin debe ser posterior a la de inicio. */
    private void aplicarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin == null || fechaInicio == null || !fechaFin.isAfter(fechaInicio)) {
            throw new BusinessRuleException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public EstadoPeriodo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPeriodo estado) {
        this.estado = estado;
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
