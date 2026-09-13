package ec.edu.scli.reservas.domain.port.out;
import java.util.Map;
public interface NotificationPort { void enviar(String token, String titulo, String cuerpo, Map<String,String> datos); }
