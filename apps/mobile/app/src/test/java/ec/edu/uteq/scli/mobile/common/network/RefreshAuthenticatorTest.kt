package ec.edu.uteq.scli.mobile.common.network

import ec.edu.uteq.scli.mobile.features.auth.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class RefreshAuthenticatorTest {
    private lateinit var server: MockWebServer
    @Before fun setup() { server = MockWebServer(); server.start() }
    @After fun close() { server.shutdown() }

    @Test fun `401 refresca y reintenta una sola vez`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))
        val repository = FakeAuthRepository()
        client(repository).newCall(Request.Builder().url(server.url("/api/v1/reservas")).build()).execute().close()
        assertEquals(1, repository.refreshes)
        assertEquals("Bearer nuevo", server.takeRequest().let { server.takeRequest() }.getHeader("Authorization"))
    }

    @Test fun `dos 401 concurrentes comparten un refresh`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(
                if (request.getHeader("Authorization") == "Bearer nuevo") 200 else 401)
        }
        val repository = FakeAuthRepository()
        val client = client(repository)
        val executor = Executors.newFixedThreadPool(2)
        val calls = (1..2).map { executor.submit { client.newCall(Request.Builder().url(server.url("/api/v1/reservas")).build()).execute().close() } }
        calls.forEach { it.get() }
        executor.shutdown()
        assertEquals(1, repository.refreshes)
    }

    @Test fun `403 no refresca`() {
        server.enqueue(MockResponse().setResponseCode(403))
        val repository = FakeAuthRepository()
        val response = client(repository).newCall(Request.Builder().url(server.url("/api/v1/reservas")).build()).execute()
        assertEquals(403, response.code); response.close()
        assertEquals(0, repository.refreshes)
    }

    private fun client(repository: AuthRepository) = OkHttpClient.Builder()
        .addInterceptor(BearerAuthInterceptor { repository.restoreSession()?.accessToken })
        .authenticator(RefreshAuthenticator(repository)).build()

    private class FakeAuthRepository : AuthRepository {
        @Volatile var session = SESSION.copy(accessToken = "anterior")
        var refreshes = 0
        override suspend fun login(username: String, password: String) = NetworkResult.Success(session)
        override fun restoreSession() = session
        override suspend fun logout() {}
        override suspend fun refreshSession(): AuthSession { Thread.sleep(30); refreshes++; return SESSION.copy(accessToken = "nuevo").also { session = it } }
    }

    companion object {
        val SESSION = AuthSession("Bearer", "token", "refresh", Long.MAX_VALUE,
            AuthUserResponse("u", "p", "user", "", "", ""))
    }
}
