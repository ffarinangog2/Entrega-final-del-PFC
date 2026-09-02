package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoRevisionPlanificacion;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revisiones_planificacion_piso")
public class RevisionPlanificacionPisoJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID planificacionId;
    private UUID pisoId;
    @Enumerated(EnumType.STRING) private EstadoRevisionPlanificacion estado;
    private String observacion;
    private UUID revisadaPorPerfilId;
    private Instant creadaEn;
    private Instant actualizadaEn;
    @Version private Long version;
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getPlanificacionId() { return planificacionId; }
    public void setPlanificacionId(UUID value) { planificacionId = value; }
    public UUID getPisoId() { return pisoId; }
    public void setPisoId(UUID value) { pisoId = value; }
    public EstadoRevisionPlanificacion getEstado() { return estado; }
    public void setEstado(EstadoRevisionPlanificacion value) { estado = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { observacion = value; }
    public UUID getRevisadaPorPerfilId() { return revisadaPorPerfilId; }
    public void setRevisadaPorPerfilId(UUID value) { revisadaPorPerfilId = value; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant value) { creadaEn = value; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public void setActualizadaEn(Instant value) { actualizadaEn = value; }
}
