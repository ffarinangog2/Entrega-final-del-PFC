package ec.edu.uteq.scli.mobile.features.institutional.data

import ec.edu.uteq.scli.mobile.common.network.GatewayClientFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InstitutionalRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: InstitutionalRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = GatewayClientFactory.createRetrofit(server.url("/").toString())
            .create(InstitutionalApi::class.java)
        repository = InstitutionalRepository(api)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    @Test
    fun `listar planificaciones interpreta el contrato real`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PLANIFICACIONES_JSON))

        val planificaciones = repository.planificaciones()

        assertEquals("plan-1", planificaciones.single().id)
        assertEquals("PROPUESTA_CAMBIO", planificaciones.single().estado)
        assertEquals("/api/v1/planificaciones", server.takeRequest().path)
    }

    @Test
    fun `coordinacion carga catalogos autorizados sin consultar usuarios globales`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PLANIFICACIONES_AGREGADAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_PERIODOS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_MATERIAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_CARRERAS_JSON))

        val coordinacion = repository.coordinacion()

        assertEquals("Programación Web", coordinacion.materias.single().nombre)
        assertEquals("Ingeniería de Software", coordinacion.carreras.single().nombre)
        assertEquals("2026-B", coordinacion.periodo.codigo)
        assertEquals(emptyList<DocentePlanificacionDto>(), coordinacion.docentes)
        val rutas = List(6) { server.takeRequest().path }
        assertEquals(true, rutas.any { it == "/api/v1/docentes/planificacion" })
        assertEquals(false, rutas.any { it?.startsWith("/api/v1/docentes?") == true })
    }

    @Test
    fun `cerrar asistencia acepta respuesta 204 sin cuerpo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        repository.cerrarSesion("sesion-1")

        assertEquals("/api/v1/asistencias/sesiones/sesion-1/cerrar", server.takeRequest().path)
    }

    @Test
    fun `historial interpreta registros propios`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(HISTORIAL_JSON))

        val historial = repository.historial()

        assertEquals("registro-1", historial.single().id)
        assertEquals("PRESENTE", historial.single().estado)
        assertEquals("/api/v1/asistencias/historial", server.takeRequest().path)
    }

    private companion object {
        const val PLANIFICACIONES_JSON = """[{"id":"plan-1","periodoId":"periodo-1","carreraId":"carrera-1","materiaId":"materia-1","docenteId":"docente-1","laboratorioId":"laboratorio-1","diaSemana":"LUNES","horaInicio":"08:00:00","horaFin":"10:00:00","estado":"PROPUESTA_CAMBIO","observacion":"Revisar horario"}]"""
        const val PLANIFICACIONES_AGREGADAS_JSON = """[{"id":"planificacion-1","periodoId":"periodo-1","carreraId":"carrera-1","estado":"BORRADOR","bloques":$PLANIFICACIONES_JSON,"revisiones":[]}]"""
        const val HISTORIAL_JSON = """[{"id":"registro-1","sesionId":"sesion-1","estudianteId":"estudiante-1","registradaEn":"2026-09-01T13:00:00Z","estado":"PRESENTE"}]"""
        const val PAGINA_MATERIAS_JSON = """{"content":[{"id":"materia-1","carreraId":"carrera-1","codigo":"PROG","nombre":"Programación Web"}],"number":0,"size":100,"totalElements":1,"totalPages":1,"numberOfElements":1,"first":true,"last":true,"empty":false}"""
        const val PAGINA_LABORATORIOS_JSON = """{"content":[{"id":"laboratorio-1","codigo":"LAB-01","nombre":"Laboratorio de Software","estado":"DISPONIBLE"}],"number":0,"size":100,"totalElements":1,"totalPages":1,"numberOfElements":1,"first":true,"last":true,"empty":false}"""
        const val PAGINA_CARRERAS_JSON = """{"content":[{"id":"carrera-1","codigo":"IS","nombre":"Ingeniería de Software"}],"number":0,"size":100,"totalElements":1,"totalPages":1,"numberOfElements":1,"first":true,"last":true,"empty":false}"""
        const val PERIODO_JSON = """{"id":"periodo-1","codigo":"2026-B","nombre":"Periodo 2026-B","estado":"ACTIVO","cicloAcademico":1}"""
        const val PAGINA_PERIODOS_JSON = """{"content":[$PERIODO_JSON],"number":0,"size":100,"totalElements":1,"totalPages":1,"numberOfElements":1,"first":true,"last":true,"empty":false}"""
    }
}
