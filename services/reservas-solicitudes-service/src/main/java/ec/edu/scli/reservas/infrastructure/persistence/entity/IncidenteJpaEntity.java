package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoIncidente;
import ec.edu.scli.reservas.domain.model.PrioridadIncidente;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity(name = "Incidente")
@Table(name = "incidentes")
public class IncidenteJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "reportante_id", nullable = false) private UUID reportanteId;
    @Column(name = "laboratorio_equipo", nullable = false, length = 200) private String laboratorioEquipo;
    @Column(nullable = false, length = 2000) private String descripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private PrioridadIncidente prioridad;
    @Column(nullable = false) private LocalDate fecha;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoIncidente estado = EstadoIncidente.REPORTADO;
    @CreationTimestamp @Column(name = "creado_en", nullable = false, updatable = false) private Instant creadoEn;
    @UpdateTimestamp @Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;
    @Version @Column(nullable = false) private Long version;

    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public UUID getReportanteId() { return reportanteId; } public void setReportanteId(UUID v) { reportanteId=v; }
    public String getLaboratorioEquipo() { return laboratorioEquipo; } public void setLaboratorioEquipo(String v) { laboratorioEquipo=v; }
    public String getDescripcion() { return descripcion; } public void setDescripcion(String v) { descripcion=v; }
    public PrioridadIncidente getPrioridad() { return prioridad; } public void setPrioridad(PrioridadIncidente v) { prioridad=v; }
    public LocalDate getFecha() { return fecha; } public void setFecha(LocalDate v) { fecha=v; }
    public EstadoIncidente getEstado() { return estado; } public void setEstado(EstadoIncidente v) { estado=v; }
    public Instant getCreadoEn() { return creadoEn; } public Instant getActualizadoEn() { return actualizadoEn; }
    public Long getVersion() { return version; } public void setVersion(Long v) { version=v; }
}
