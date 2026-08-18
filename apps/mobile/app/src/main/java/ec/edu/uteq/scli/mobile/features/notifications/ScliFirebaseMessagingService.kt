package ec.edu.uteq.scli.mobile.features.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Recibe los mensajes push de Firebase Cloud Messaging.
 *
 * No funciona todavía: falta agregar `apps/mobile/app/google-services.json`
 * (ver apps/mobile/README.md) y habilitar Firebase Cloud Messaging API (V1)
 * en el proyecto de Firebase Console. El código queda listo para cuando eso
 * exista.
 */
class ScliFirebaseMessagingService : FirebaseMessagingService() {

    private val notificationHelper by lazy { NotificationHelper(applicationContext) }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag("FCM").i("Nuevo token de registro FCM: %s", token)
        // TODO: enviar el token al backend cuando exista el endpoint correspondiente.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.tag("FCM").i("Mensaje push recibido de: %s", message.from ?: "desconocido")

        val titulo = message.notification?.title ?: message.data["titulo"] ?: "SCLI"
        val cuerpo = message.notification?.body ?: message.data["cuerpo"] ?: ""

        notificationHelper.mostrar(titulo, cuerpo)
    }
}
