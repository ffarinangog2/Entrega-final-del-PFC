package ec.edu.uteq.scli.mobile.features.reservas.data

import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.common.network.DataSource
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaDao
import ec.edu.uteq.scli.mobile.features.reservas.data.local.ReservaEntity
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.ReservasApi
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa
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
    fun `listar con respuesta con forma inesperada devuelve respuesta_gateway_invalida`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        assertEquals(NetworkResult.Failure(null, "respuesta_gateway_invalida"), repository.listar())
    }

    @Test
    fun `listar solicitudes exitoso mapea la pagina de dominio`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_SOLICITUDES_JSON))

        val result = repository.listarSolicitudes()

        assertTrue(result is NetworkResult.Success)
        assertEquals("solicitud-1", (result as NetworkResult.Success).value.contenido.single().id)
    }

    @Test
    fun `historial exitoso mapea la pagina de dominio`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_HISTORIAL_JSON))

        val result = repository.historial("solicitud-1")

        assertTrue(result is NetworkResult.Success)
        assertEquals("APROBADA", (result as NetworkResult.Success).value.contenido.single().estadoNuevo)
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

    @Test
    fun `detalle con respuesta con forma inesperada devuelve respuesta_gateway_invalida`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        assertEquals(NetworkResult.Failure(null, "respuesta_gateway_invalida"), repository.obtener("reserva-1"))
    }

    @Test
    fun `crear solicitud exitosa mapea la respuesta al dominio`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.crearSolicitud(NUEVA_SOLICITUD, "idem-1")

        assertTrue(result is NetworkResult.Success)
        assertEquals("solicitud-1", (result as NetworkResult.Success).value.id)
        val request = server.takeRequest()
        assertEquals("idem-1", request.getHeader("Idempotency-Key"))
    }

    @Test
    fun `actualizar solicitud exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.actualizarSolicitud("solicitud-1", ACTUALIZACION)

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `cancelar solicitud exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.cancelarSolicitud("solicitud-1", "ya no se necesita")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `cancelar reserva exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RESERVA_JSON))

        val result = repository.cancelarReserva("reserva-1", "cambio de horario")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `poner en revision mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.ponerEnRevision("solicitud-1")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `aprobar solicitud exitosa mapea la reserva creada`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RESERVA_JSON))

        val result = repository.aprobar("solicitud-1", "responsable-1", "ok", "idem-2")

        assertTrue(result is NetworkResult.Success)
        assertEquals("reserva-1", (result as NetworkResult.Success).value.id)
    }

    @Test
    fun `rechazar solicitud exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.rechazar("solicitud-1", "no cumple requisitos")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `proponer alternativa exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.proponer(
            "solicitud-1",
            PropuestaAlternativa("laboratorio-2", "2026-08-21", "09:00", "11:00", "cambio"),
        )

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `responder propuesta aceptando usa el endpoint de aceptar`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.responderPropuesta("solicitud-1", aceptar = true, comentario = "de acuerdo")

        assertTrue(result is NetworkResult.Success)
        assertEquals("/api/v1/solicitudes/solicitud-1/propuesta/aceptar", server.takeRequest().path)
    }

    @Test
    fun `responder propuesta rechazando usa el endpoint de rechazar`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SOLICITUD_JSON))

        val result = repository.responderPropuesta("solicitud-1", aceptar = false, comentario = "no procede")

        assertTrue(result is NetworkResult.Success)
        assertEquals("/api/v1/solicitudes/solicitud-1/propuesta/rechazar", server.takeRequest().path)
    }

    @Test
    fun `consultar disponibilidad exitosa mapea la respuesta`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DISPONIBILIDAD_JSON))

        val result = repository.consultarDisponibilidad("laboratorio-1", "2026-08-20", "08:00", "10:00")

        assertTrue(result is NetworkResult.Success)
        assertTrue((result as NetworkResult.Success).value.disponible)
    }

    @Test
    fun `respuesta http de error en solicitud generica no lanza excepcion`() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("{}"))

        val result = repository.cancelarSolicitud("solicitud-1", "conflicto")

        assertEquals(NetworkResult.Failure(409, "gateway_http_409"), result)
    }

    @Test
    fun `fallo de red en solicitud generica devuelve gateway_no_disponible`() = runTest {
        server.shutdown()

        val result = repository.cancelarSolicitud("solicitud-1", "sin red")

        assertEquals(NetworkResult.Failure(null, "gateway_no_disponible"), result)
    }

    @Test
    fun `respuesta con forma inesperada devuelve respuesta_gateway_invalida`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = repository.cancelarSolicitud("solicitud-1", "cualquiera")

        assertEquals(NetworkResult.Failure(null, "respuesta_gateway_invalida"), result)
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
        const val SOLICITUD_JSON = """
            {"id":"solicitud-1","solicitanteId":"solicitante-1","docenteId":"docente-1",
            "laboratorioId":"laboratorio-1","materiaId":"materia-1","periodoLectivoId":"periodo-1",
            "fechaReserva":"2026-08-20","horaInicio":"08:00","horaFin":"10:00","numeroParticipantes":20,
            "motivo":"Clase","observacion":null,"estado":"PENDIENTE","reservaId":null,
            "creadaEn":"2026-08-18T10:00:00Z","actualizadaEn":"2026-08-18T10:00:00Z","version":0}
        """
        const val PAGINA_SOLICITUDES_JSON = """
            {
              "contenido": [{
                "id":"solicitud-1","solicitanteId":"solicitante-1","docenteId":"docente-1",
                "laboratorioId":"laboratorio-1","materiaId":"materia-1","periodoLectivoId":"periodo-1",
                "fechaReserva":"2026-08-20","horaInicio":"08:00","horaFin":"10:00","numeroParticipantes":20,
                "motivo":"Clase","observacion":null,"estado":"PENDIENTE","reservaId":null,
                "creadaEn":"2026-08-18T10:00:00Z","actualizadaEn":"2026-08-18T10:00:00Z","version":0
              }],
              "pagina": 0, "tamanio": 20, "totalElementos": 1, "totalPaginas": 1,
              "primera": true, "ultima": true
            }
        """
        const val PAGINA_HISTORIAL_JSON = """
            {
              "contenido": [{
                "id":"historial-1","estadoAnterior":"PENDIENTE","estadoNuevo":"APROBADA",
                "usuarioId":"usuario-1","comentario":"ok","creadoEn":"2026-08-18T12:00:00Z"
              }],
              "pagina": 0, "tamanio": 50, "totalElementos": 1, "totalPaginas": 1,
              "primera": true, "ultima": true
            }
        """
        const val DISPONIBILIDAD_JSON = """
            {"laboratorioId":"laboratorio-1","fecha":"2026-08-20","horaInicio":"08:00",
            "horaFin":"10:00","disponible":true,"motivo":null}
        """
        val NUEVA_SOLICITUD = NuevaSolicitudReserva(
            "solicitante-1", "docente-1", "laboratorio-1", "materia-1", "periodo-1",
            "2026-08-20", "08:00", "10:00", 20, "Clase", null,
        )
        val ACTUALIZACION = ActualizacionSolicitudReserva(
            "docente-1", "laboratorio-1", "materia-1", "periodo-1",
            "2026-08-20", "08:00", "10:00", 20, "Clase", null,
        )
    }
}
