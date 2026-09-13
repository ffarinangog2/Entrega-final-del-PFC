package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoSolicitudRetiro;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decisiones_retiro_planificacion")
public class DecisionRetiroPlanificacionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID solicitudRetiroId;
    private UUID pisoId;
    private UUID revisadaPorPerfilId;
    @Enumerated(EnumType.STRING) private EstadoSolicitudRetiro estado = EstadoSolicitudRetiro.PENDIENTE;
    @Column(columnDefinition = "TEXT") private String observacion;
    private Instant creadaEn;
    private Instant resueltaEn;
    @Version private Long version;
    @PrePersist void crear() { if (creadaEn == null) creadaEn = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getSolicitudRetiroId() { return solicitudRetiroId; }
    public void setSolicitudRetiroId(UUID value) { solicitudRetiroId = value; }
    public UUID getPisoId() { return pisoId; }
    public void setPisoId(UUID value) { pisoId = value; }
    public UUID getRevisadaPorPerfilId() { return revisadaPorPerfilId; }
    public void setRevisadaPorPerfilId(UUID value) { revisadaPorPerfilId = value; }
    public EstadoSolicitudRetiro getEstado() { return estado; }
    public void setEstado(EstadoSolicitudRetiro value) { estado = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { observacion = value; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(Instant value) { resueltaEn = value; }
}
