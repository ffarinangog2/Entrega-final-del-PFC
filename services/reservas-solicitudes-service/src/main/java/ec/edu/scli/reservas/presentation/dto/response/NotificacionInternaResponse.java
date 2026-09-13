package ec.edu.scli.reservas.presentation.dto.response;
import java.time.Instant; import java.util.UUID;
public record NotificacionInternaResponse(UUID id,String titulo,String cuerpo,String tipo,UUID referenciaId,boolean leida,Instant creadaEn){}
