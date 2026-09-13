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
    fun `revisionPiso consulta unicamente planificaciones agregadas y laboratorios autorizados`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PLANIFICACIONES_AGREGADAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))

        val revision = repository.revisionPiso()

        assertEquals(1, revision.planificaciones.size)
        assertEquals("plan-1", revision.planificaciones.single().id)
        assertEquals(1, revision.laboratorios.size)
        assertEquals("LAB-01", revision.laboratorios.single().codigo)
        assertEquals("planificacion-1", revision.planificacion?.id)
        assertEquals(emptyList<MateriaPlanificacionDto>(), revision.materias)
        assertEquals(emptyList<DocentePlanificacionDto>(), revision.docentes)
        assertEquals(emptyList<CarreraPlanificacionDto>(), revision.carreras)

        val rutas = List(2) { server.takeRequest().path }
        assertEquals("/api/v1/planificaciones-agregadas", rutas[0])
        assertEquals(true, rutas[1]?.startsWith("/api/v1/laboratorios") == true)
        assertEquals(false, rutas.any { it?.contains("materias") == true })
        assertEquals(false, rutas.any { it?.contains("carreras") == true })
        assertEquals(false, rutas.any { it?.contains("periodos-lectivos") == true })
        assertEquals(false, rutas.any { it?.contains("docentes") == true })
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

    @Test
    fun `docencia consulta periodoActual y luego miHorarioDocente con periodoId sin llamar endpoints redundantes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PERIODO_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(MI_HORARIO_DOCENTE_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_MATERIAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))

        val docencia = repository.docencia("perfil-docente-1")

        assertEquals("periodo-1", docencia.periodo?.id)
        assertEquals(1, docencia.horarios.size)
        assertEquals("bloque-1", docencia.horarios.single().id)
        assertEquals("materia-1", docencia.horarios.single().materiaId)
        assertEquals("laboratorio-1", docencia.horarios.single().laboratorioId)
        assertEquals("CONFIRMADA", docencia.horarios.single().estado)
        assertEquals("LUNES", docencia.horarios.single().diaSemana)
        assertEquals("07:30:00", docencia.horarios.single().horaInicio)
        assertEquals("09:30:00", docencia.horarios.single().horaFin)

        val rutas = List(4) { server.takeRequest().path }
        assertEquals("/api/v1/periodos-lectivos/actual", rutas[0])
        assertEquals("/api/v1/asistencias/mi-horario-docente?periodoId=periodo-1", rutas[1])
        assertEquals(true, rutas.any { it?.startsWith("/api/v1/materias") == true })
        assertEquals(true, rutas.any { it?.startsWith("/api/v1/laboratorios") == true })
        assertEquals(false, rutas.any { it?.startsWith("/api/v1/horarios/docente") == true })
        assertEquals(false, rutas.any { it?.startsWith("/api/v1/docentes/perfil") == true })
    }

    @Test
    fun `docencia sin periodo actual no llama miHorarioDocente y devuelve horario vacio fail closed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"status":404,"message":"No hay período lectivo activo"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_MATERIAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))

        val docencia = repository.docencia("perfil-docente-1")

        assertEquals(null, docencia.periodo)
        assertEquals(emptyList<PlanificacionBloqueDto>(), docencia.horarios)
        assertEquals(1, docencia.materias.size)
        assertEquals(1, docencia.laboratorios.size)

        val rutas = List(3) { server.takeRequest().path }
        assertEquals("/api/v1/periodos-lectivos/actual", rutas[0])
        assertEquals(false, rutas.any { it?.contains("mi-horario-docente") == true })
        assertEquals(false, rutas.any { it?.startsWith("/api/v1/horarios/docente") == true })
        assertEquals(false, rutas.any { it?.startsWith("/api/v1/docentes/perfil") == true })
    }

    @Test
    fun `estudianteHorario consulta periodoActual y luego miHorario con periodoId exacto sin solicitar estudianteId ni endpoints redundantes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PERIODO_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(MI_HORARIO_ESTUDIANTE_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_MATERIAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))

        val data = repository.estudianteHorario()

        assertEquals("periodo-1", data.periodo?.id)
        assertEquals(1, data.horarios.size)
        assertEquals("bloque-est-1", data.horarios.single().id)
        assertEquals("materia-1", data.horarios.single().materiaId)
        assertEquals("laboratorio-1", data.horarios.single().laboratorioId)
        assertEquals("CONFIRMADA", data.horarios.single().estado)
        assertEquals("LUNES", data.horarios.single().diaSemana)
        assertEquals("07:30:00", data.horarios.single().horaInicio)
        assertEquals("09:30:00", data.horarios.single().horaFin)
        assertEquals(2, data.horarios.single().nivel)

        val rutas = List(4) { server.takeRequest().path }
        assertEquals("/api/v1/periodos-lectivos/actual", rutas[0])
        assertEquals("/api/v1/asistencias/mi-horario?periodoId=periodo-1", rutas[1])
        assertEquals(true, rutas.any { it?.startsWith("/api/v1/materias") == true })
        assertEquals(true, rutas.any { it?.startsWith("/api/v1/laboratorios") == true })
        assertEquals(false, rutas.any { it?.contains("estudiante") == true })
        assertEquals(false, rutas.any { it?.contains("perfil") == true })
        assertEquals(4, rutas.size)
    }

    @Test
    fun `estudianteHorario sin periodo actual no llama miHorario y devuelve horario vacio fail closed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"status":404,"message":"No hay período lectivo activo"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_MATERIAS_JSON))
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGINA_LABORATORIOS_JSON))

        val data = repository.estudianteHorario()

        assertEquals(null, data.periodo)
        assertEquals(emptyList<PlanificacionBloqueDto>(), data.horarios)
        assertEquals(1, data.materias.size)
        assertEquals(1, data.laboratorios.size)

        val rutas = List(3) { server.takeRequest().path }
        assertEquals("/api/v1/periodos-lectivos/actual", rutas[0])
        assertEquals(false, rutas.any { it?.contains("mi-horario") == true })
        assertEquals(false, rutas.any { it?.contains("estudiante") == true })
        assertEquals(false, rutas.any { it?.contains("perfil") == true })
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
        const val MI_HORARIO_DOCENTE_JSON = """[{"id":"bloque-1","planificacionId":"plan-1","nivel":1,"periodoId":"periodo-1","carreraId":"carrera-1","materiaId":"materia-1","docenteId":"docente-1","laboratorioId":"laboratorio-1","diaSemana":"LUNES","horaInicio":"07:30:00","horaFin":"09:30:00","estado":"CONFIRMADA","observacion":null}]"""
        const val MI_HORARIO_ESTUDIANTE_JSON = """[{"id":"bloque-est-1","planificacionId":"plan-1","nivel":2,"periodoId":"periodo-1","carreraId":"carrera-1","materiaId":"materia-1","docenteId":"docente-1","laboratorioId":"laboratorio-1","diaSemana":"LUNES","horaInicio":"07:30:00","horaFin":"09:30:00","estado":"CONFIRMADA","observacion":null}]"""
    }
}
