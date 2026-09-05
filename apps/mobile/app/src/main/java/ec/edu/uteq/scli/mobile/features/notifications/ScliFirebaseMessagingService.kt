package ec.edu.uteq.scli.mobile.features.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import ec.edu.uteq.scli.mobile.ScliMobileApplication
import kotlinx.coroutines.*

/**
 * Recibe los mensajes push de Firebase Cloud Messaging.
 *
 * El registro del dispositivo se conserva localmente hasta que exista una
 * sesión autenticada y entonces se sincroniza con el backend.
 */
class ScliFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationHelper by lazy { NotificationHelper(applicationContext) }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val registrar = (application as ScliMobileApplication).container.deviceTokenRegistrar
        registrar.guardarPendiente(token)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { registrar.registrarPendiente() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.tag("FCM").i("Mensaje push recibido de: %s", message.from ?: "desconocido")

        val titulo = message.notification?.title ?: message.data["titulo"] ?: "SCLI"
        val cuerpo = message.notification?.body ?: message.data["cuerpo"] ?: ""

        notificationHelper.mostrar(titulo, cuerpo)
    }
}
