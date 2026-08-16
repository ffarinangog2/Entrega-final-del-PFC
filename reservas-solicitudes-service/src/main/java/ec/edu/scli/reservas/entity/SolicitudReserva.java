package ec.edu.scli.reservas.entity;

import ec.edu.scli.reservas.enums.EstadoSolicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "solicitudes_reserva")
public class SolicitudReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    @Column(name = "docente_id", nullable = false)
    private UUID docenteId;

    @Column(name = "laboratorio_id", nullable = false)
    private UUID laboratorioId;

    @Column(name = "materia_id", nullable = false)
    private UUID materiaId;

    @Column(name = "periodo_lectivo_id", nullable = false)
    private UUID periodoLectivoId;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "numero_participantes", nullable = false)
    private Integer numeroParticipantes;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "clave_idempotencia", length = 100)
    private String claveIdempotencia;

    @CreationTimestamp
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @UpdateTimestamp
    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private Reserva reserva;

    @OneToMany(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private List<HistorialSolicitud> historial = new ArrayList<>();

    protected SolicitudReserva() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(UUID solicitanteId) { this.solicitanteId = solicitanteId; }
    public UUID getDocenteId() { return docenteId; }
    public void setDocenteId(UUID docenteId) { this.docenteId = docenteId; }
    public UUID getLaboratorioId() { return laboratorioId; }
    public void setLaboratorioId(UUID laboratorioId) { this.laboratorioId = laboratorioId; }
    public UUID getMateriaId() { return materiaId; }
    public void setMateriaId(UUID materiaId) { this.materiaId = materiaId; }
    public UUID getPeriodoLectivoId() { return periodoLectivoId; }
    public void setPeriodoLectivoId(UUID periodoLectivoId) { this.periodoLectivoId = periodoLectivoId; }
    public LocalDate getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDate fechaReserva) { this.fechaReserva = fechaReserva; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public Integer getNumeroParticipantes() { return numeroParticipantes; }
    public void setNumeroParticipantes(Integer numeroParticipantes) { this.numeroParticipantes = numeroParticipantes; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public List<HistorialSolicitud> getHistorial() { return historial; }
    public void setHistorial(List<HistorialSolicitud> historial) { this.historial = historial; }
}
