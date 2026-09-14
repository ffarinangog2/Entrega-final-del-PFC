package ec.edu.scli.reservas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "idempotencia_creacion_solicitudes")
public class IdempotenciaCreacionSolicitudJpaEntity {
    @Id @Column(nullable = false, length = 100) private String clave;
    @Column(nullable = false, length = 50) private String operacion;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "solicitud_id") private UUID solicitudId;

    protected IdempotenciaCreacionSolicitudJpaEntity() { }
    public String getClave() { return clave; }
    public String getOperacion() { return operacion; }
    public UUID getActorId() { return actorId; }
    public String getPayloadHash() { return payloadHash; }
    public UUID getSolicitudId() { return solicitudId; }
}
