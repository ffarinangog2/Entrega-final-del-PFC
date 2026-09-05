package ec.edu.scli.reservas.presentation.dto.request;
import java.util.UUID;
public record AbrirSesionAsistenciaRequest(UUID reservaId, UUID bloqueId) {
 public AbrirSesionAsistenciaRequest(UUID reservaId){this(reservaId,null);}
}
