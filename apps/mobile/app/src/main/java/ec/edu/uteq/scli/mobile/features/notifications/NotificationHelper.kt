package ec.edu.uteq.scli.mobile.features.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ec.edu.uteq.scli.mobile.R
import timber.log.Timber
import kotlin.random.Random

/**
 * Punto único para crear el canal de notificaciones y mostrar notificaciones
 * locales, tanto desde [ScliFirebaseMessagingService] (push) como desde la
 * feature de Incidentes (aviso local al crear uno nuevo).
 */
class NotificationHelper(private val context: Context) {

    private val channelId: String = context.getString(R.string.notification_channel_id)

    init {
        createChannelIfNeeded()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.notification_channel_nombre),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_descripcion)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun mostrar(titulo: String, cuerpo: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.tag("NotificationHelper").w("Permiso POST_NOTIFICATIONS no concedido, no se muestra la notificación")
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
