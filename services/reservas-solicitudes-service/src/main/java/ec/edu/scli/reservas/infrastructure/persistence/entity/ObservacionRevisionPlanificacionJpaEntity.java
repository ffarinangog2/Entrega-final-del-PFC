package ec.edu.scli.reservas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "observaciones_revision_planificacion")
public class ObservacionRevisionPlanificacionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID revisionId;
    private UUID bloqueId;
    private UUID laboratorioPropuestoId;
    private String observacion;
    private Instant creadaEn;
    public UUID getId() { return id; }
    public UUID getRevisionId() { return revisionId; }
    public void setRevisionId(UUID value) { revisionId = value; }
    public UUID getBloqueId() { return bloqueId; }
    public void setBloqueId(UUID value) { bloqueId = value; }
    public UUID getLaboratorioPropuestoId() { return laboratorioPropuestoId; }
    public void setLaboratorioPropuestoId(UUID value) { laboratorioPropuestoId = value; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String value) { observacion = value; }
    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant value) { creadaEn = value; }
}
