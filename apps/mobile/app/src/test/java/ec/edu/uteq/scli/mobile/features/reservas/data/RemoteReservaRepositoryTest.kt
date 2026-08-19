package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteReservaRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteReservaRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = GatewayClientFactory.createRetrofit(server.url("/").toString())
            .create(ReservasApi::class.java)
        repository = RemoteReservaRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `listar reservas devuelve pagina de dominio y usa ruta real`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_JSON))

        val result = repository.listar()

        assertTrue(result is NetworkResult.Success)
        val pagina = (result as NetworkResult.Success).value
        assertEquals("reserva-1", pagina.contenido.single().id)
        assertEquals("PROGRAMADA", pagina.contenido.single().estado)
        val request = server.takeRequest()
        assertEquals("/api/v1/reservas?pagina=0&tamanio=20", request.path)
    }

    @Test
    fun `error HTTP se expone sin intentar mapear cuerpo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))

        val result = repository.listar()

        assertEquals(NetworkResult.Failure(503, "gateway_http_503"), result)
    }

    private companion object {
        const val PAGINA_JSON = """
            {
              "contenido": [{
                "id": "reserva-1",
                "solicitudId": "solicitud-1",
                "laboratorioId": "laboratorio-1",
                "responsableId": "responsable-1",
                "fechaReserva": "2026-08-20",
                "horaInicio": "08:00:00",
                "horaFin": "10:00:00",
                "estado": "PROGRAMADA",
                "codigoReserva": "RES-001",
                "creadaEn": "2026-08-18T10:00:00Z",
                "actualizadaEn": "2026-08-18T10:00:00Z",
                "version": 0
              }],
              "pagina": 0,
              "tamanio": 20,
              "totalElementos": 1,
              "totalPaginas": 1,
              "primera": true,
              "ultima": true
            }
        """
    }
}
