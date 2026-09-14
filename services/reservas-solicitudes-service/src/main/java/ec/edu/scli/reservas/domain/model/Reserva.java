package ec.edu.scli.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Modelo de dominio limpio de una reserva confirmada. */
public class Reserva {
    private UUID id;
    private UUID solicitudId;
    private UUID laboratorioId;
    private UUID pisoId;
    private UUID responsableId;
    private LocalDate fechaReserva;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoReserva estado = EstadoReserva.PROGRAMADA;
    private String codigoReserva;
    private Instant creadaEn;
    private Instant actualizadaEn;
    private Long version;

    public Reserva() { }
    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public UUID getSolicitudId() { return solicitudId; } public void setSolicitudId(UUID v) { solicitudId=v; }
    public UUID getLaboratorioId() { return laboratorioId; } public void setLaboratorioId(UUID v) { laboratorioId=v; }
    public UUID getPisoId() { return pisoId; } public void setPisoId(UUID v) { pisoId=v; }
    public UUID getResponsableId() { return responsableId; } public void setResponsableId(UUID v) { responsableId=v; }
    public LocalDate getFechaReserva() { return fechaReserva; } public void setFechaReserva(LocalDate v) { fechaReserva=v; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime v) { horaInicio=v; }
    public LocalTime getHoraFin() { return horaFin; } public void setHoraFin(LocalTime v) { horaFin=v; }
    public EstadoReserva getEstado() { return estado; } public void setEstado(EstadoReserva v) { estado=v; }
    public String getCodigoReserva() { return codigoReserva; } public void setCodigoReserva(String v) { codigoReserva=v; }
    public Instant getCreadaEn() { return creadaEn; } public void setCreadaEn(Instant v) { creadaEn=v; }
    public Instant getActualizadaEn() { return actualizadaEn; } public void setActualizadaEn(Instant v) { actualizadaEn=v; }
    public Long getVersion() { return version; } public void setVersion(Long v) { version=v; }
}
