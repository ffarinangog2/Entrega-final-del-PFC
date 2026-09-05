package ec.edu.uteq.scli.mobile.features.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NotificationHelperTest {
    @Test
    @Config(sdk = [28])
    fun `creates channel and publishes notification on supported device`() {
        val application = RuntimeEnvironment.getApplication()
        val manager = application.getSystemService(NotificationManager::class.java)
        val helper = NotificationHelper(application)

        assertNotNull(manager.getNotificationChannel("incidentes_channel"))

        helper.mostrar("Reserva aprobada", "Laboratorio disponible")

        val notifications = shadowOf(manager).allNotifications
        assertEquals(1, notifications.size)
        assertEquals("Reserva aprobada", notifications.single().extras.getString("android.title"))
        assertEquals("Laboratorio disponible", notifications.single().extras.getString("android.text"))
    }

    @Test
    @Config(sdk = [33])
    fun `does not publish when notification permission is denied`() {
        val application = RuntimeEnvironment.getApplication()
        val manager = application.getSystemService(NotificationManager::class.java)
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationHelper(application).mostrar("Incidente", "Sin permiso")

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    @Config(sdk = [33])
    fun `publishes when notification permission is granted`() {
        val application = RuntimeEnvironment.getApplication()
        val manager = application.getSystemService(NotificationManager::class.java)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationHelper(application).mostrar("Incidente", "Creado correctamente")

        assertEquals(1, shadowOf(manager).allNotifications.size)
    }
}
