package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoSolicitudRetiro;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitudes_retiro_planificacion")
public class SolicitudRetiroPlanificacionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID planificacionId;
    private UUID solicitantePerfilId;
    @Column(columnDefinition = "TEXT") private String motivo;
    @Enumerated(EnumType.STRING) private EstadoSolicitudRetiro estado = EstadoSolicitudRetiro.PENDIENTE;
    private Instant creadaEn;
    private Instant resueltaEn;
    @Version private Long version;
    @PrePersist void crear() { if (creadaEn == null) creadaEn = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getPlanificacionId() { return planificacionId; }
    public void setPlanificacionId(UUID value) { planificacionId = value; }
    public UUID getSolicitantePerfilId() { return solicitantePerfilId; }
    public void setSolicitantePerfilId(UUID value) { solicitantePerfilId = value; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String value) { motivo = value; }
    public EstadoSolicitudRetiro getEstado() { return estado; }
    public void setEstado(EstadoSolicitudRetiro value) { estado = value; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(Instant value) { resueltaEn = value; }
}
