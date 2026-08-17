package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.*;

@Entity(name = "SolicitudReserva")
@Table(name = "solicitudes_reserva")
public class SolicitudReservaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "solicitante_id", nullable = false) private UUID solicitanteId;
    @Column(name = "docente_id", nullable = false) private UUID docenteId;
    @Column(name = "laboratorio_id", nullable = false) private UUID laboratorioId;
    @Column(name = "materia_id", nullable = false) private UUID materiaId;
    @Column(name = "periodo_lectivo_id", nullable = false) private UUID periodoLectivoId;
    @Column(name = "fecha_reserva", nullable = false) private LocalDate fechaReserva;
    @Column(name = "hora_inicio", nullable = false) private LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false) private LocalTime horaFin;
    @Column(name = "numero_participantes", nullable = false) private Integer numeroParticipantes;
    @Column(name = "motivo", nullable = false, length = 500) private String motivo;
    @Column(name = "observacion", columnDefinition = "TEXT") private String observacion;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;
    @Column(name = "clave_idempotencia", length = 100) private String claveIdempotencia;
    @CreationTimestamp @Column(name = "creada_en", nullable = false, updatable = false) private Instant creadaEn;
    @UpdateTimestamp @Column(name = "actualizada_en", nullable = false) private Instant actualizadaEn;
    @Version @Column(name = "version", nullable = false) private Long version;
    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY) private ReservaJpaEntity reserva;
    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY) private List<HistorialSolicitudJpaEntity> historial = new ArrayList<>();

    public SolicitudReservaJpaEntity() { }
    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public UUID getSolicitanteId() { return solicitanteId; } public void setSolicitanteId(UUID v) { solicitanteId=v; }
    public UUID getDocenteId() { return docenteId; } public void setDocenteId(UUID v) { docenteId=v; }
    public UUID getLaboratorioId() { return laboratorioId; } public void setLaboratorioId(UUID v) { laboratorioId=v; }
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
    public ReservaJpaEntity getReserva() { return reserva; } public void setReserva(ReservaJpaEntity v) { reserva=v; }
    public List<HistorialSolicitudJpaEntity> getHistorial() { return historial; } public void setHistorial(List<HistorialSolicitudJpaEntity> v) { historial=v; }
}
