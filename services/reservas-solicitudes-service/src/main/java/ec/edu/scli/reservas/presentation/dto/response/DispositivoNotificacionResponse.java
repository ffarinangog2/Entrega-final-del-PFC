package ec.edu.scli.reservas.presentation.dto.response;
import java.time.Instant; import java.util.UUID;
public record DispositivoNotificacionResponse(UUID id,String plataforma,boolean activo,Instant creadoEn,Instant actualizadoEn) { }
