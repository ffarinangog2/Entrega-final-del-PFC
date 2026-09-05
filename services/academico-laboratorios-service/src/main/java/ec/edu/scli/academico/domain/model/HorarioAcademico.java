package ec.edu.scli.academico.domain.model;

import ec.edu.scli.academico.enums.DiaSemana;
import ec.edu.scli.academico.domain.exception.BusinessRuleException;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio de HorarioAcademico, sin anotaciones JPA. */
public class HorarioAcademico {

    private UUID id;
    private UUID materiaId;
    private UUID periodoLectivoId;
    private UUID laboratorioId;
    // UUID externo (usuarios-service), sin llave foránea entre microservicios.
    private UUID docenteId;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String paralelo;
    private boolean activo;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public HorarioAcademico() {
    }

    public static HorarioAcademico nuevo(
            UUID materiaId,
            UUID periodoLectivoId,
            UUID laboratorioId,
            UUID docenteId,
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            String paralelo
    ) {
        HorarioAcademico horario = new HorarioAcademico();
        horario.materiaId = materiaId;
        horario.periodoLectivoId = periodoLectivoId;
        horario.laboratorioId = laboratorioId;
        horario.docenteId = docenteId;
        horario.diaSemana = diaSemana;
        horario.aplicarHoras(horaInicio, horaFin);
        horario.paralelo = paralelo;
        horario.activo = true;
        return horario;
    }

    /** Regla de negocio: la hora de fin debe ser posterior a la de inicio. */
    private void aplicarHoras(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null || horaFin == null || !horaFin.isAfter(horaInicio)) {
            throw new BusinessRuleException(
                    "La hora de fin debe ser posterior a la hora de inicio");
        }
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMateriaId() {
        return materiaId;
    }

    public UUID getPeriodoLectivoId() {
        return periodoLectivoId;
    }

    public UUID getLaboratorioId() {
        return laboratorioId;
    }

    public UUID getDocenteId() {
        return docenteId;
    }

    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public String getParalelo() {
        return paralelo;
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
