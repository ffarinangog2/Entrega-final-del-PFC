package ec.edu.scli.reservas.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Modelo de dominio limpio de una transición de solicitud. */
public class HistorialSolicitud {
    private UUID id, solicitudId, usuarioAccionId;
    private EstadoSolicitud estadoAnterior, estadoNuevo;
    private String comentario;
    private Instant fechaHora;
    public HistorialSolicitud() { }
    public UUID getId() { return id; } public void setId(UUID v) { id=v; }
    public UUID getSolicitudId() { return solicitudId; } public void setSolicitudId(UUID v) { solicitudId=v; }
    public EstadoSolicitud getEstadoAnterior() { return estadoAnterior; } public void setEstadoAnterior(EstadoSolicitud v) { estadoAnterior=v; }
    public EstadoSolicitud getEstadoNuevo() { return estadoNuevo; } public void setEstadoNuevo(EstadoSolicitud v) { estadoNuevo=v; }
    public UUID getUsuarioAccionId() { return usuarioAccionId; } public void setUsuarioAccionId(UUID v) { usuarioAccionId=v; }
    public String getComentario() { return comentario; } public void setComentario(String v) { comentario=v; }
    public Instant getFechaHora() { return fechaHora; } public void setFechaHora(Instant v) { fechaHora=v; }
}
