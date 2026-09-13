package ec.edu.scli.reservas.domain.model;

import java.time.*;
import java.util.UUID;

/** Modelo de dominio limpio de una solicitud de reserva. */
public class SolicitudReserva {
    private UUID id, solicitanteId, docenteId, laboratorioId, pisoId, materiaId, periodoLectivoId, reservaId;
    private UUID propuestaLaboratorioId;
    private LocalDate fechaReserva;
    private LocalTime horaInicio, horaFin;
    private LocalDate propuestaFecha;
    private LocalTime propuestaHoraInicio, propuestaHoraFin;
    private Integer numeroParticipantes;
    private String motivo, observacion, claveIdempotencia, propuestaObservacion;
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    private Instant creadaEn, actualizadaEn;
    private Long version;
    public SolicitudReserva() { }
    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public UUID getSolicitanteId() { return solicitanteId; } public void setSolicitanteId(UUID v) { solicitanteId=v; }
    public UUID getDocenteId() { return docenteId; } public void setDocenteId(UUID v) { docenteId=v; }
    public UUID getLaboratorioId() { return laboratorioId; } public void setLaboratorioId(UUID v) { laboratorioId=v; }
    public UUID getPisoId() { return pisoId; } public void setPisoId(UUID v) { pisoId=v; }
    public UUID getMateriaId() { return materiaId; } public void setMateriaId(UUID v) { materiaId=v; }
    public UUID getPeriodoLectivoId() { return periodoLectivoId; } public void setPeriodoLectivoId(UUID v) { periodoLectivoId=v; }
    public LocalDate getFechaReserva() { return fechaReserva; } public void setFechaReserva(LocalDate v) { fechaReserva=v; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime v) { horaInicio=v; }
    public LocalTime getHoraFin() { return horaFin; } public void setHoraFin(LocalTime v) { horaFin=v; }
    public Integer getNumeroParticipantes() { return numeroParticipantes; } public void setNumeroParticipantes(Integer v) { numeroParticipantes=v; }
    public String getMotivo() { return motivo; } public void setMotivo(String v) { motivo=v; }
    public String getObservacion() { return observacion; } public void setObservacion(String v) { observacion=v; }
    public EstadoSolicitud getEstado() { return estado; } public void setEstado(EstadoSolicitud v) { estado=v; }
    public String getClaveIdempotencia() { return claveIdempotencia; } public void setClaveIdempotencia(String v) { claveIdempotencia=v; }
    public Instant getCreadaEn() { return creadaEn; } public void setCreadaEn(Instant v) { creadaEn=v; }
    public Instant getActualizadaEn() { return actualizadaEn; } public void setActualizadaEn(Instant v) { actualizadaEn=v; }
    public Long getVersion() { return version; } public void setVersion(Long v) { version=v; }
    public UUID getReservaId() { return reservaId; } public void setReservaId(UUID v) { reservaId=v; }
    public UUID getPropuestaLaboratorioId() { return propuestaLaboratorioId; } public void setPropuestaLaboratorioId(UUID v) { propuestaLaboratorioId=v; }
    public LocalDate getPropuestaFecha() { return propuestaFecha; } public void setPropuestaFecha(LocalDate v) { propuestaFecha=v; }
    public LocalTime getPropuestaHoraInicio() { return propuestaHoraInicio; } public void setPropuestaHoraInicio(LocalTime v) { propuestaHoraInicio=v; }
    public LocalTime getPropuestaHoraFin() { return propuestaHoraFin; } public void setPropuestaHoraFin(LocalTime v) { propuestaHoraFin=v; }
    public String getPropuestaObservacion() { return propuestaObservacion; } public void setPropuestaObservacion(String v) { propuestaObservacion=v; }
}
