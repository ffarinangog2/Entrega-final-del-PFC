package ec.edu.uteq.scli.mobile.features.auth.data

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteAuthRepositoryTest {
    private val api = mockk<AuthApi>()
    private val storage = mockk<SecureTokenStorage>(relaxed = true)

    @Test fun `refresh exitoso guarda tokens rotados`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.refresh(RefreshRequest("refresh-anterior")) } returns Response.success(LOGIN)
        val refreshed = RemoteAuthRepository(api, storage, clock = { 1000 }).refreshSession()
        assertEquals("access-nuevo", refreshed?.accessToken)
        verify { storage.save(match { it.refreshToken == "refresh-nuevo" }) }
    }

    @Test fun `refresh rechazado limpia sesion`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.refresh(any()) } returns Response.error(401, "{}".toResponseBody())
        var expired = false
        val repository = RemoteAuthRepository(api, storage).also { it.onSessionExpired { expired = true } }
        assertNull(repository.refreshSession())
        verify { storage.clear() }
        assertTrue(expired)
    }

    @Test fun `refresh sin sesion previa no llama a la api`() = runTest {
        every { storage.read() } returns null
        val repository = RemoteAuthRepository(api, storage)
        assertNull(repository.refreshSession())
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test fun `refresh con error de red limpia sesion`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.refresh(any()) } throws IOException("sin red")
        var expired = false
        val repository = RemoteAuthRepository(api, storage).also { it.onSessionExpired { expired = true } }
        assertNull(repository.refreshSession())
        verify { storage.clear() }
        assertTrue(expired)
    }

    @Test fun `login exitoso guarda la sesion con expiracion calculada`() = runTest {
        coEvery { api.login(LoginRequest("admin", "Admin123!")) } returns Response.success(LOGIN)
        val repository = RemoteAuthRepository(api, storage, clock = { 1_000L })

        val result = repository.login("admin", "Admin123!")

        assertTrue(result is NetworkResult.Success)
        val session = (result as NetworkResult.Success).value
        assertEquals("access-nuevo", session.accessToken)
        assertEquals(1_000L + 300_000L, session.expiresAtMillis)
        verify { storage.save(session) }
    }

    @Test fun `login con credenciales invalidas devuelve fallo 401`() = runTest {
        coEvery { api.login(any()) } returns Response.error(401, "{}".toResponseBody())
        val result = RemoteAuthRepository(api, storage).login("admin", "mala")
        assertEquals(NetworkResult.Failure(401, "credenciales_invalidas"), result)
    }

    @Test fun `login con credenciales invalidas devuelve fallo 403`() = runTest {
        coEvery { api.login(any()) } returns Response.error(403, "{}".toResponseBody())
        val result = RemoteAuthRepository(api, storage).login("admin", "mala")
        assertEquals(NetworkResult.Failure(403, "credenciales_invalidas"), result)
    }

    @Test fun `login con cuenta bloqueada devuelve fallo 423`() = runTest {
        coEvery { api.login(any()) } returns Response.error(423, "{}".toResponseBody())
        val result = RemoteAuthRepository(api, storage).login("admin", "cualquiera")
        assertEquals(NetworkResult.Failure(423, "cuenta_bloqueada"), result)
    }

    @Test fun `login con error de servicio devuelve fallo generico`() = runTest {
        coEvery { api.login(any()) } returns Response.error(500, "{}".toResponseBody())
        val result = RemoteAuthRepository(api, storage).login("admin", "cualquiera")
        assertEquals(NetworkResult.Failure(500, "servicio_no_disponible"), result)
    }

    @Test fun `login con fallo de red devuelve error_red`() = runTest {
        coEvery { api.login(any()) } throws IOException("sin conexion")
        val result = RemoteAuthRepository(api, storage).login("admin", "cualquiera")
        assertEquals(NetworkResult.Failure(null, "error_red"), result)
    }

    @Test fun `login con excepcion inesperada devuelve servicio_no_disponible`() = runTest {
        coEvery { api.login(any()) } throws IllegalStateException("respuesta invalida")
        val result = RemoteAuthRepository(api, storage).login("admin", "cualquiera")
        assertEquals(NetworkResult.Failure(null, "servicio_no_disponible"), result)
    }

    @Test fun `restoreSession devuelve la sesion si no ha expirado`() = runTest {
        every { storage.read() } returns SESSION
        val session = RemoteAuthRepository(api, storage, clock = { 0L }).restoreSession()
        assertEquals(SESSION, session)
    }

    @Test fun `restoreSession devuelve null si la sesion expiro`() = runTest {
        every { storage.read() } returns SESSION.copy(expiresAtMillis = 100L)
        val session = RemoteAuthRepository(api, storage, clock = { 200L }).restoreSession()
        assertNull(session)
    }

    @Test fun `restoreSession devuelve null si no hay sesion guardada`() = runTest {
        every { storage.read() } returns null
        val session = RemoteAuthRepository(api, storage).restoreSession()
        assertNull(session)
    }

    @Test fun `refresh con excepcion inesperada limpia sesion`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.refresh(any()) } throws IllegalStateException("respuesta invalida")
        var expired = false
        val repository = RemoteAuthRepository(api, storage).also { it.onSessionExpired { expired = true } }
        assertNull(repository.refreshSession())
        verify { storage.clear() }
        assertTrue(expired)
    }

    @Test fun `logout revoca remotamente y limpia el almacenamiento`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.logout(RefreshRequest("refresh-anterior")) } returns Response.success(Unit)
        RemoteAuthRepository(api, storage).logout()
        coVerify { api.logout(RefreshRequest("refresh-anterior")) }
        verify { storage.clear() }
    }

    @Test fun `logout limpia almacenamiento aunque falle la red`() = runTest {
        every { storage.read() } returns SESSION
        coEvery { api.logout(any()) } throws IOException("sin red")
        RemoteAuthRepository(api, storage).logout()
        verify { storage.clear() }
    }

    companion object {
        val USER = AuthUserResponse("u", "p", "user", "", "", "")
        val SESSION = AuthSession("Bearer", "access-anterior", "refresh-anterior", Long.MAX_VALUE, USER)
        val LOGIN = LoginResponse("Bearer", "access-nuevo", "refresh-nuevo", 300, USER)
    }
}
