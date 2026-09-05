package ec.edu.scli.reservas.infrastructure.notification;
import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
@ConditionalOnProperty(name = "app.notifications.firebase.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpNotificationAdapter implements NotificationPort {
    public void enviar(String token,String titulo,String cuerpo,Map<String,String> datos) { }
}
