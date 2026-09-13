package ec.edu.uteq.scli.mobile.common.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BearerAuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `agrega Authorization Bearer cuando existe access token`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWithToken("access-token-compartido")

        client.newCall(Request.Builder().url(server.url("/api/v1/solicitudes")).build())
            .execute().use { response -> assertEquals(200, response.code) }

        assertEquals(
            "Bearer access-token-compartido",
            server.takeRequest().getHeader("Authorization"),
        )
    }

    @Test
    fun `no inventa Authorization cuando no existe access token`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val client = clientWithToken(null)

        client.newCall(Request.Builder().url(server.url("/api/v1/reservas")).build())
            .execute().use { response -> assertEquals(200, response.code) }

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    private fun clientWithToken(token: String?) = OkHttpClient.Builder()
        .addInterceptor(BearerAuthInterceptor { token })
        .build()
}
