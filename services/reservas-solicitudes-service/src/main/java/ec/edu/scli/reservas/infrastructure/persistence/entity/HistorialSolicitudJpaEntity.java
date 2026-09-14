package ec.edu.scli.reservas.infrastructure.persistence.entity;

import ec.edu.scli.reservas.domain.model.EstadoSolicitud;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "HistorialSolicitud")
@Table(name = "historial_solicitudes")
public class HistorialSolicitudJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false) private SolicitudReservaJpaEntity solicitud;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 30) private EstadoSolicitud estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", nullable = false, length = 30) private EstadoSolicitud estadoNuevo;
    @Column(name = "usuario_accion_id", nullable = false) private UUID usuarioAccionId;
    @Column(name = "comentario", columnDefinition = "TEXT") private String comentario;
    @CreationTimestamp @Column(name = "fecha_hora", nullable = false, updatable = false) private Instant fechaHora;
    public HistorialSolicitudJpaEntity() { }
    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public SolicitudReservaJpaEntity getSolicitud() { return solicitud; } public void setSolicitud(SolicitudReservaJpaEntity v) { solicitud=v; }
    public EstadoSolicitud getEstadoAnterior() { return estadoAnterior; } public void setEstadoAnterior(EstadoSolicitud v) { estadoAnterior=v; }
    public EstadoSolicitud getEstadoNuevo() { return estadoNuevo; } public void setEstadoNuevo(EstadoSolicitud v) { estadoNuevo=v; }
    public UUID getUsuarioAccionId() { return usuarioAccionId; } public void setUsuarioAccionId(UUID v) { usuarioAccionId=v; }
    public String getComentario() { return comentario; } public void setComentario(String v) { comentario=v; }
    public Instant getFechaHora() { return fechaHora; } public void setFechaHora(Instant v) { fechaHora=v; }
}
