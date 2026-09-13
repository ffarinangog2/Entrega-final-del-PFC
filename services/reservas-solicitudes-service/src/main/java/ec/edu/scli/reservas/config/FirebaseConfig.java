package ec.edu.scli.reservas.config;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
@Configuration @ConditionalOnProperty(name="app.notifications.firebase.enabled",havingValue="true")
public class FirebaseConfig {
    @Bean FirebaseApp firebaseApp(@Value("${app.notifications.firebase.credentials-base64}") String encoded) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
        if (encoded == null || encoded.isBlank()) throw new IllegalStateException("FIREBASE_CREDENTIALS_BASE64 es obligatorio cuando Firebase está habilitado");
        return FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))).build());
    }
    @Bean FirebaseMessaging firebaseMessaging(FirebaseApp app){return FirebaseMessaging.getInstance(app);}
}
