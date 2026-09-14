package ec.edu.uteq.scli.mobile.features.notifications.data

import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NotificationsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = GatewayClientFactory.createRetrofit(server.url("/").toString())
            .create(NotificationsApi::class.java)
        repository = NotificationsRepository(api)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `A - GET notificaciones mapea correctamente titulo, cuerpo, tipo, referenciaId, leida, creadaEn`() = runTest {
        val json = """
            [
                {
                    "id": "notif-uuid-1",
                    "titulo": "Asistencia disponible",
                    "cuerpo": "Actividad de laboratorio",
                    "tipo": "ASISTENCIA_ABIERTA",
                    "referenciaId": "sesion-uuid-123",
                    "leida": false,
                    "creadaEn": "2026-09-08T01:30:00Z"
                },
                {
                    "id": "notif-uuid-2",
                    "titulo": "Incidente actualizado",
                    "cuerpo": "El incidente ahora está RESUELTO",
                    "tipo": "INCIDENTE_ACTUALIZADO",
                    "referenciaId": null,
                    "leida": true,
                    "creadaEn": "2026-09-08T00:00:00Z"
                }
            ]
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.listar()

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)

        val first = list[0]
        assertEquals("notif-uuid-1", first.id)
        assertEquals("Asistencia disponible", first.titulo)
        assertEquals("Actividad de laboratorio", first.cuerpo)
        assertEquals("ASISTENCIA_ABIERTA", first.tipo)
        assertEquals("sesion-uuid-123", first.referenciaId)
        assertFalse(first.leida)
        assertEquals("2026-09-08T01:30:00Z", first.creadaEn)

        val second = list[1]
        assertEquals("notif-uuid-2", second.id)
        assertNull(second.referenciaId)
        assertTrue(second.leida)

        val req = server.takeRequest()
        assertEquals("/api/v1/notificaciones", req.path)
        assertEquals("GET", req.method)
    }

    @Test
    fun `B - GET no-leidas mapea cantidad`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"cantidad": 7}"""))

        val result = repository.contarNoLeidas()

        assertTrue(result.isSuccess)
        assertEquals(7L, result.getOrThrow())

        val req = server.takeRequest()
        assertEquals("/api/v1/notificaciones/no-leidas", req.path)
        assertEquals("GET", req.method)
    }

    @Test
    fun `C - POST id leer marca correctamente`() = runTest {
        val json = """
            {
                "id": "notif-123",
                "titulo": "Solicitud aprobada",
                "cuerpo": "Su solicitud fue aceptada",
                "tipo": "SOLICITUD",
                "referenciaId": "sol-456",
                "leida": true,
                "creadaEn": "2026-09-08T02:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.marcarLeida("notif-123")

        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("notif-123", item.id)
        assertTrue(item.leida)

        val req = server.takeRequest()
        assertEquals("/api/v1/notificaciones/notif-123/leer", req.path)
        assertEquals("POST", req.method)
    }

    @Test
    fun `D - POST leer-todas acepta 204`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.marcarTodasLeidas()

        assertTrue(result.isSuccess)

        val req = server.takeRequest()
        assertEquals("/api/v1/notificaciones/leer-todas", req.path)
        assertEquals("POST", req.method)
    }

    @Test
    fun `error de red se captura limpiamente como Result failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.listar()

        assertTrue(result.isFailure)
    }
}
