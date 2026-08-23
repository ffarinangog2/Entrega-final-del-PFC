package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.common.network.DataSource
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaDao
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaEntity
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    private lateinit var dao: ReservaDao

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = GatewayClientFactory.createRetrofit(server.url("/").toString())
            .create(ReservasApi::class.java)
        dao = mockk(relaxed = true)
        repository = RemoteReservaRepository(api, dao)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `listar reservas devuelve pagina de dominio y usa ruta real`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_JSON))

        val result = repository.listar()

        assertTrue(result is NetworkResult.Success)
        val pagina = (result as NetworkResult.Success).value
        assertEquals("reserva-1", pagina.contenido.single().id)
        assertEquals("PROGRAMADA", pagina.contenido.single().estado)
        coVerify { dao.guardarTodas(match { it.single().id == "reserva-1" }) }
        val request = server.takeRequest()
        assertEquals("/api/v1/reservas?pagina=0&tamanio=20", request.path)
    }

    @Test
    fun `HTTP 401 no usa cache`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val result = repository.listar()

        assertEquals(NetworkResult.Failure(401, "gateway_http_401"), result)
        coVerify(exactly = 0) { dao.obtenerTodas() }
    }

    @Test
    fun `HTTP 403 no usa cache`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))

        val result = repository.listar()

        assertEquals(NetworkResult.Failure(403, "gateway_http_403"), result)
        coVerify(exactly = 0) { dao.obtenerTodas() }
    }

    @Test
    fun `fallo de red devuelve listado cacheado`() = runTest {
        coEvery { dao.obtenerTodas() } returns listOf(ENTITY)
        server.shutdown()

        val result = repository.listar()

        assertTrue(result is NetworkResult.Success)
        result as NetworkResult.Success
        assertEquals(DataSource.CACHE, result.source)
        assertEquals("reserva-1", result.value.contenido.single().id)
    }

    @Test
    fun `fallo de red sin cache conserva error de conectividad`() = runTest {
        coEvery { dao.obtenerTodas() } returns emptyList()
        server.shutdown()

        assertEquals(NetworkResult.Failure(null, "gateway_no_disponible"), repository.listar())
    }

    @Test
    fun `detalle remoto exitoso actualiza cache`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RESERVA_JSON))

        val result = repository.obtener("reserva-1")

        assertTrue(result is NetworkResult.Success)
        coVerify { dao.guardar(match { it.id == "reserva-1" }) }
    }

    @Test
    fun `fallo de red devuelve detalle cacheado`() = runTest {
        coEvery { dao.obtenerPorId("reserva-1") } returns ENTITY
        server.shutdown()

        val result = repository.obtener("reserva-1")

        assertTrue(result is NetworkResult.Success && result.source == DataSource.CACHE)
    }

    @Test
    fun `HTTP 404 de detalle no usa cache`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

        assertEquals(NetworkResult.Failure(404, "gateway_http_404"), repository.obtener("reserva-1"))
        coVerify(exactly = 0) { dao.obtenerPorId(any()) }
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
        const val RESERVA_JSON = """
            {"id":"reserva-1","solicitudId":"solicitud-1","laboratorioId":"laboratorio-1",
            "responsableId":"responsable-1","fechaReserva":"2026-08-20","horaInicio":"08:00:00",
            "horaFin":"10:00:00","estado":"PROGRAMADA","codigoReserva":"RES-001",
            "creadaEn":"2026-08-18T10:00:00Z","actualizadaEn":"2026-08-18T10:00:00Z","version":0}
        """
        val ENTITY = ReservaEntity(
            "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1", "2026-08-20",
            "08:00:00", "10:00:00", "PROGRAMADA", "RES-001",
            "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
        )
    }
}
