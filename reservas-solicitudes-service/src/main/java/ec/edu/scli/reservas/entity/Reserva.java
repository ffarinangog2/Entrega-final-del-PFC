package ec.edu.scli.reservas.entity;

import ec.edu.scli.reservas.enums.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudReserva solicitud;

    @Column(name = "laboratorio_id", nullable = false)
    private UUID laboratorioId;

    @Column(name = "responsable_id", nullable = false)
    private UUID responsableId;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoReserva estado = EstadoReserva.PROGRAMADA;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 50)
    private String codigoReserva;

    @CreationTimestamp
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @UpdateTimestamp
    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Reserva() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SolicitudReserva getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudReserva solicitud) { this.solicitud = solicitud; }
    public UUID getLaboratorioId() { return laboratorioId; }
    public void setLaboratorioId(UUID laboratorioId) { this.laboratorioId = laboratorioId; }
    public UUID getResponsableId() { return responsableId; }
    public void setResponsableId(UUID responsableId) { this.responsableId = responsableId; }
    public LocalDate getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDate fechaReserva) { this.fechaReserva = fechaReserva; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant actualizadaEn) { this.actualizadaEn = actualizadaEn; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
