package ec.edu.uteq.scli.mobile.features.notifications

import android.content.Context
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeviceTokenRegistrarTest {
    private val api = mockk<DeviceRegistrationApi>()
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("scli_fcm", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `alta pendiente registra token y lo conserva como asociado`() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(Unit)
        val registrar = DeviceTokenRegistrar(context, api)
        registrar.guardarPendiente("token-a")
        registrar.registrarPendiente()
        coVerify { api.registrar(RegistrarDispositivoRequest("token-a")) }
    }

    @Test fun `baja repetida es segura`() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(Unit)
        coEvery { api.desregistrar(any()) } returns Response.success(Unit)
        val registrar = DeviceTokenRegistrar(context, api)
        registrar.guardarPendiente("token-a")
        registrar.registrarPendiente()
        registrar.desregistrarActual()
        registrar.desregistrarActual()
        coVerify(exactly = 2) { api.desregistrar(DesregistrarDispositivoRequest("token-a")) }
    }

    @Test fun `baja offline queda pendiente y siguiente login reasocia token`() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(Unit)
        coEvery { api.desregistrar(any()) } throws java.io.IOException("sin red") andThen Response.success(Unit)
        val registrar = DeviceTokenRegistrar(context, api)
        registrar.guardarPendiente("token-a")
        registrar.registrarPendiente()
        registrar.desregistrarActual()
        registrar.guardarPendiente("token-a")
        registrar.registrarPendiente()
        coVerify(exactly = 2) { api.registrar(RegistrarDispositivoRequest("token-a")) }
        coVerify(exactly = 2) { api.desregistrar(DesregistrarDispositivoRequest("token-a")) }
    }

    @Test fun `token rotado se registra para la sesion vigente`() = runTest {
        coEvery { api.registrar(any()) } returns Response.success(Unit)
        val registrar = DeviceTokenRegistrar(context, api)
        registrar.guardarPendiente("token-nuevo")
        registrar.registrarPendiente()
        coVerify { api.registrar(RegistrarDispositivoRequest("token-nuevo")) }
    }
}
