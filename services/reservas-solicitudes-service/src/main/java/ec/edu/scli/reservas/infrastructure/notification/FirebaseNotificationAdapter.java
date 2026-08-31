package ec.edu.scli.reservas.infrastructure.notification;
import com.google.firebase.messaging.*;
import ec.edu.scli.reservas.domain.port.out.NotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component @ConditionalOnProperty(name="app.notifications.firebase.enabled",havingValue="true")
public class FirebaseNotificationAdapter implements NotificationPort {
    private final FirebaseMessaging messaging;
    public FirebaseNotificationAdapter(FirebaseMessaging messaging){this.messaging=messaging;}
    public void enviar(String token,String titulo,String cuerpo,Map<String,String> datos) {
        try { messaging.send(Message.builder().setToken(token)
                .setNotification(Notification.builder().setTitle(titulo).setBody(cuerpo).build())
                .putAllData(datos).build()); }
        catch (FirebaseMessagingException exception) { throw new IllegalStateException("No se pudo enviar la notificación",exception); }
    }
}
