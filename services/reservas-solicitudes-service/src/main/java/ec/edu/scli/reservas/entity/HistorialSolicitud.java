package ec.edu.scli.reservas.entity;

import ec.edu.scli.reservas.enums.EstadoSolicitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historial_solicitudes")
public class HistorialSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudReserva solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 30)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoSolicitud estadoNuevo;

    @Column(name = "usuario_accion_id", nullable = false)
    private UUID usuarioAccionId;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @CreationTimestamp
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    protected HistorialSolicitud() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SolicitudReserva getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudReserva solicitud) { this.solicitud = solicitud; }
    public EstadoSolicitud getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoSolicitud estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoSolicitud getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoSolicitud estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public UUID getUsuarioAccionId() { return usuarioAccionId; }
    public void setUsuarioAccionId(UUID usuarioAccionId) { this.usuarioAccionId = usuarioAccionId; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
}
