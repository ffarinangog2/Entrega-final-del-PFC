package ec.edu.uteq.scli.mobile.features.notifications

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import ec.edu.uteq.scli.mobile.common.navigation.MobileNavigationAccess
import ec.edu.uteq.scli.mobile.features.notifications.data.ConteoNoLeidasResponse
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificationsApi
import ec.edu.uteq.scli.mobile.features.notifications.data.NotificationsRepository
import ec.edu.uteq.scli.mobile.features.notifications.presentation.NotificationsViewModel
import ec.edu.uteq.scli.mobile.features.notifications.presentation.resolveNotificationDestination
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PushNotificationNavigationTest {

    private lateinit var context: Context
    private val api = mockk<NotificationsApi>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // A. parser data: referenciaId directo
    @Test
    fun `caso A - parser data con referenciaId directo`() {
        val data = mapOf("tipo" to "SOLICITUD", "referenciaId" to "ref-123")
        val payload = parsePushNavigationPayload(data)
        assertNotNull(payload)
        assertEquals("SOLICITUD", payload?.tipo)
        assertEquals("ref-123", payload?.referenciaId)
    }

    // B. fallback solicitudId
    @Test
    fun `caso B - fallback solicitudId cuando falta referenciaId`() {
        val data = mapOf("tipo" to "SOLICITUD", "solicitudId" to "sol-456")
        val payload = parsePushNavigationPayload(data)
        assertNotNull(payload)
        assertEquals("SOLICITUD", payload?.tipo)
        assertEquals("sol-456", payload?.referenciaId)
    }

    // C. fallback planificacionId
    @Test
    fun `caso C - fallback planificacionId`() {
        val data = mapOf("tipo" to "PLANIFICACION", "planificacionId" to "plan-789")
        val payload = parsePushNavigationPayload(data)
        assertNotNull(payload)
        assertEquals("PLANIFICACION", payload?.tipo)
        assertEquals("plan-789", payload?.referenciaId)
    }

    // D. fallback incidenteId
    @Test
    fun `caso D - fallback incidenteId`() {
        val data = mapOf("tipo" to "INCIDENTE_ACTUALIZADO", "incidenteId" to "inc-999")
        val payload = parsePushNavigationPayload(data)
        assertNotNull(payload)
        assertEquals("INCIDENTE_ACTUALIZADO", payload?.tipo)
        assertEquals("inc-999", payload?.referenciaId)
    }

    // E. tipo ausente: payload invalido o null
    @Test
    fun `caso E - tipo ausente o vacio produce null`() {
        assertNull(parsePushNavigationPayload(mapOf("referenciaId" to "ref-123")))
        assertNull(parsePushNavigationPayload(mapOf("tipo" to "   ", "referenciaId" to "ref-123")))
        assertNull(parsePushNavigationPayload(null as Map<String, String>?))
    }

    // F. NotificationHelper genera Intent con extras
    @Test
    fun `caso F - NotificationHelper genera Intent con extras normalizados`() {
        val helper = NotificationHelper(context)
        val payload = PushNavigationPayload(tipo = "SOLICITUD", referenciaId = "sol-101")
        val intent = helper.crearIntent(payload)

        assertEquals("SOLICITUD", intent.getStringExtra(EXTRA_PUSH_TIPO))
        assertEquals("sol-101", intent.getStringExtra(EXTRA_PUSH_REFERENCIA_ID))
    }

    // G. dos pushes diferentes producen PendingIntent y requestCode independientes
    @Test
    fun `caso G - dos pushes diferentes producen requestCode independientes`() {
        val p1 = PushNavigationPayload("SOLICITUD", "sol-1")
        val p2 = PushNavigationPayload("SOLICITUD", "sol-2")
        val p3 = PushNavigationPayload("RESERVA", "res-1")

        val code1 = calcularPushRequestCode(p1)
        val code2 = calcularPushRequestCode(p2)
        val code3 = calcularPushRequestCode(p3)

        assertNotEquals(code1, code2)
        assertNotEquals(code1, code3)
        assertNotEquals(code2, code3)
    }

    // H. cold start procesa Intent inicial
    @Test
    fun `caso H - cold start procesa Intent inicial correctamente`() {
        val manager = PushNavigationManager()
        val initialIntent = Intent().apply {
            putExtra("tipo", "SOLICITUD")
            putExtra("referenciaId", "sol-cold")
        }
        val payload = parsePushNavigationPayload(initialIntent)
        assertNotNull(payload)
        manager.emitPayload(payload!!)

        assertEquals("sol-cold", manager.pendingPayload.value?.referenciaId)
        assertEquals("SOLICITUD", manager.pendingPayload.value?.tipo)
    }

    // I. onNewIntent procesa nuevo push con Activity viva
    @Test
    fun `caso I - onNewIntent procesa nuevo push con Activity viva`() {
        val manager = PushNavigationManager()
        val newIntent = Intent().apply {
            putExtra(EXTRA_PUSH_TIPO, "RESERVA")
            putExtra(EXTRA_PUSH_REFERENCIA_ID, "res-live")
        }
        val payload = parsePushNavigationPayload(newIntent)
        assertNotNull(payload)
        manager.emitPayload(payload!!)

        assertEquals("RESERVA", manager.pendingPayload.value?.tipo)
        assertEquals("res-live", manager.pendingPayload.value?.referenciaId)
    }

    // J. evento one-shot no se procesa dos veces
    @Test
    fun `caso J - evento one-shot se consume una sola vez`() {
        val manager = PushNavigationManager()
        manager.emitPayload(PushNavigationPayload("SOLICITUD", "sol-one-shot"))

        val primero = manager.consumePayload()
        assertNotNull(primero)
        assertEquals("sol-one-shot", primero?.referenciaId)

        val segundo = manager.consumePayload()
        assertNull(segundo)
        assertNull(manager.pendingPayload.value)
    }

    // K. sesion valida: usa resolveNotificationDestination
    @Test
    fun `caso K - sesion valida resuelve destino correctamente con el resolver`() {
        val access = MobileNavigationAccess(reservas = true)
        val payload = PushNavigationPayload("SOLICITUD", "sol-valid")
        val destino = resolveNotificationDestination(
            tipo = payload.tipo,
            referenciaId = payload.referenciaId,
            access = access,
        )
        assertEquals("solicitudes/sol-valid", destino)
    }

    // L. sin sesion: descarta payload
    @Test
    fun `caso L - sin sesion descarta el payload pendiente`() {
        val manager = PushNavigationManager()
        manager.emitPayload(PushNavigationPayload("SOLICITUD", "sol-unauth"))
        assertEquals("sol-unauth", manager.pendingPayload.value?.referenciaId)

        // Al no haber sesion valida, se llama a clear()
        manager.clear()
        assertNull(manager.pendingPayload.value)
        assertNull(manager.consumePayload())
    }

    // M. logout: descarta payload pendiente
    @Test
    fun `caso M - logout limpia cualquier payload en cola`() {
        val manager = PushNavigationManager()
        manager.emitPayload(PushNavigationPayload("RESERVA", "res-logout"))

        // Al ejecutar logout()
        manager.clear()
        assertNull(manager.pendingPayload.value)
    }

    // N. rol sin permiso: no navega
    @Test
    fun `caso N - rol sin permiso devuelve null`() {
        val accessEstudiante = MobileNavigationAccess(reservas = false, estudiante = true)
        val destino = resolveNotificationDestination("SOLICITUD", "sol-no-access", accessEstudiante)
        assertNull(destino)
    }

    // O. tipo desconocido: no navega
    @Test
    fun `caso O - tipo desconocido devuelve null`() {
        val access = MobileNavigationAccess(reservas = true)
        val destino = resolveNotificationDestination("DESCONOCIDO_XYZ", "123", access)
        assertNull(destino)
    }

    // P. foreground push refresca badge sin polling
    @Test
    fun `caso P - notificarPushRecibido emite en pushTrigger`() = runTest {
        val repo = NotificationsRepository(api)
        var triggerEmitido = false

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.pushTrigger.collect {
                triggerEmitido = true
            }
        }

        repo.notificarPushRecibido()
        assertTrue(triggerEmitido)
        job.cancel()
    }

    // Q. ON_RESUME refresca badge
    @Test
    fun `caso Q - ON_RESUME refresca contador no leidas`() = runTest {
        coEvery { api.noLeidas() } returns ConteoNoLeidasResponse(cantidad = 7L)
        val repo = NotificationsRepository(api)
        val viewModel = NotificationsViewModel(repo)

        // Simula la llamada realizada en el callback de ON_RESUME
        viewModel.cargarNoLeidas()

        assertEquals(7L, viewModel.uiState.value.cantidadNoLeidas)
    }
}
