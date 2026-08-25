package ec.edu.uteq.scli.mobile.features.auth.data

import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

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

    companion object {
        val USER = AuthUserResponse("u", "p", "user", "", "", "")
        val SESSION = AuthSession("Bearer", "access-anterior", "refresh-anterior", Long.MAX_VALUE, USER)
        val LOGIN = LoginResponse("Bearer", "access-nuevo", "refresh-nuevo", 300, USER)
    }
}
