package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoReserva;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity(name = "Reserva")
@Table(name = "reservas")
public class ReservaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudReservaJpaEntity solicitud;
    @Column(name = "laboratorio_id", nullable = false) private UUID laboratorioId;
    @Column(name = "responsable_id", nullable = false) private UUID responsableId;
    @Column(name = "fecha_reserva", nullable = false) private LocalDate fechaReserva;
    @Column(name = "hora_inicio", nullable = false) private LocalTime horaInicio;
    @Column(name = "hora_fin", nullable = false) private LocalTime horaFin;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false, length = 30)
    private EstadoReserva estado = EstadoReserva.PROGRAMADA;
    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 50) private String codigoReserva;
    @CreationTimestamp @Column(name = "creada_en", nullable = false, updatable = false) private Instant creadaEn;
    @UpdateTimestamp @Column(name = "actualizada_en", nullable = false) private Instant actualizadaEn;
    @Version @Column(name = "version", nullable = false) private Long version;

    public ReservaJpaEntity() { }
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public SolicitudReservaJpaEntity getSolicitud() { return solicitud; } public void setSolicitud(SolicitudReservaJpaEntity solicitud) { this.solicitud = solicitud; }
    public UUID getLaboratorioId() { return laboratorioId; } public void setLaboratorioId(UUID value) { this.laboratorioId = value; }
    public UUID getResponsableId() { return responsableId; } public void setResponsableId(UUID value) { this.responsableId = value; }
    public LocalDate getFechaReserva() { return fechaReserva; } public void setFechaReserva(LocalDate value) { this.fechaReserva = value; }
    public LocalTime getHoraInicio() { return horaInicio; } public void setHoraInicio(LocalTime value) { this.horaInicio = value; }
    public LocalTime getHoraFin() { return horaFin; } public void setHoraFin(LocalTime value) { this.horaFin = value; }
    public EstadoReserva getEstado() { return estado; } public void setEstado(EstadoReserva value) { this.estado = value; }
    public String getCodigoReserva() { return codigoReserva; } public void setCodigoReserva(String value) { this.codigoReserva = value; }
    public Instant getCreadaEn() { return creadaEn; } public void setCreadaEn(Instant value) { this.creadaEn = value; }
    public Instant getActualizadaEn() { return actualizadaEn; } public void setActualizadaEn(Instant value) { this.actualizadaEn = value; }
    public Long getVersion() { return version; } public void setVersion(Long value) { this.version = value; }
}
