package ec.edu.scli.reservas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "IdempotenciaAprobacion")
@Table(name = "idempotencia_aprobaciones")
public class IdempotenciaAprobacionJpaEntity {

    @Id
    @Column(name = "clave", length = 100, nullable = false)
    private String clave;

    @Column(name = "operacion", length = 50, nullable = false)
    private String operacion;

    @Column(name = "solicitud_id", nullable = false)
    private UUID solicitudId;

    @Column(name = "reserva_id")
    private UUID reservaId;

    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "completada_en", insertable = false)
    private Instant completadaEn;

    public String getClave() { return clave; }
    public String getOperacion() { return operacion; }
    public UUID getSolicitudId() { return solicitudId; }
    public UUID getReservaId() { return reservaId; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getCompletadaEn() { return completadaEn; }
}
