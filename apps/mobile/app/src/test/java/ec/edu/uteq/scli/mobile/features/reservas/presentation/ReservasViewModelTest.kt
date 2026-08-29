package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.HistorialSolicitud
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReservasViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `carga inicial exitosa muestra reservas`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        assertEquals(listOf(RESERVA), viewModel.uiState.value.reservas)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `error de carga se expone en el estado`() = runTest {
        val repository = FakeReservaRepository().apply {
            listadoResult = NetworkResult.Failure(503, "gateway_http_503")
        }
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        assertEquals("No fue posible procesar la solicitud.", viewModel.uiState.value.error)
    }

    @Test
    fun `refresh vuelve a consultar sin activar loading principal`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        viewModel.cargarReservas(esRefresco = true)
        assertTrue(viewModel.uiState.value.refrescando)
        assertFalse(viewModel.uiState.value.cargando)
        runCurrent()
        assertEquals(2, repository.listados)
        assertFalse(viewModel.uiState.value.refrescando)
    }

    @Test
    fun `detalle consulta por id real`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()
        assertEquals(RESERVA.id, repository.detalleId)
        assertEquals(RESERVA, viewModel.uiState.value.seleccionada)
    }

    @Test
    fun `cancelacion programada actualiza el detalle`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()
        viewModel.cancelarReserva("Cambio de horario")
        runCurrent()
        assertEquals("Cambio de horario", repository.motivoCancelacion)
        assertEquals("CANCELADA", viewModel.uiState.value.seleccionada?.estado)
        assertTrue(viewModel.uiState.value.cancelacionExitosa)
    }

    @Test
    fun `actuar aprobar recarga el detalle en caso de exito`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("aprobar", "responsable-1", "ok")
        runCurrent()

        assertEquals(1, repository.aprobaciones)
        assertEquals(2, repository.detallesSolicitud)
    }

    @Test
    fun `actuar rechazar actualiza la solicitud seleccionada`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("rechazar", "responsable-1", "no cumple")
        runCurrent()

        assertEquals("no cumple", repository.comentarioRechazo)
        assertEquals("RECHAZADA", viewModel.uiState.value.solicitudSeleccionada?.estado)
    }

    @Test
    fun `actuar sin solicitud seleccionada no hace nada`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)

        viewModel.actuarSolicitud("rechazar", "responsable-1", "no cumple")
        runCurrent()

        assertEquals(0, repository.rechazos)
    }

    @Test
    fun `actuar con accion desconocida no hace nada`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("desconocida", "responsable-1")
        runCurrent()

        assertEquals(SOLICITUD, viewModel.uiState.value.solicitudSeleccionada)
    }

    @Test
    fun `actuar poner en revision invoca al repositorio`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("revision", "responsable-1")
        runCurrent()

        assertEquals(1, repository.revisiones)
        assertEquals("EN_REVISION", viewModel.uiState.value.solicitudSeleccionada?.estado)
    }

    @Test
    fun `actuar cancelar invoca al repositorio`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("cancelar", "responsable-1", "ya no aplica")
        runCurrent()

        assertEquals(1, repository.cancelacionesSolicitud)
        assertEquals("CANCELADA", viewModel.uiState.value.solicitudSeleccionada?.estado)
    }

    @Test
    fun `actuar aceptar propuesta invoca responder con true`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("aceptar", "responsable-1", "de acuerdo")
        runCurrent()

        assertEquals(1, repository.propuestasAceptadas)
        assertEquals(0, repository.propuestasRechazadas)
    }

    @Test
    fun `actuar rechazar propuesta invoca responder con false`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("rechazar_propuesta", "responsable-1", "no procede")
        runCurrent()

        assertEquals(1, repository.propuestasRechazadas)
        assertEquals(0, repository.propuestasAceptadas)
    }

    @Test
    fun `actuar proponer envia la propuesta alternativa`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud(
            "proponer",
            "responsable-1",
            propuesta = PropuestaAlternativa(
                "laboratorio-2", "2026-08-21", "09:00", "11:00", null,
            ),
        )
        runCurrent()

        assertEquals(1, repository.propuestas)
        assertEquals("PROPUESTA", viewModel.uiState.value.solicitudSeleccionada?.estado)
    }

    @Test
    fun `cargarSolicitud con fallo expone el mensaje traducido`() = runTest {
        val repository = FakeReservaRepository().apply {
            obtenerSolicitudResult = NetworkResult.Failure(404, "no_encontrada")
        }
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)

        viewModel.cargarSolicitud("solicitud-inexistente")
        runCurrent()

        assertFalse(viewModel.uiState.value.cargando)
        assertEquals("No se encontró el recurso.", viewModel.uiState.value.error)
    }

    @Test
    fun `cargarDetalle con fallo expone el mensaje del servidor`() = runTest {
        val repository = FakeReservaRepository().apply {
            obtenerResult = NetworkResult.Failure(404, "reserva_no_encontrada")
        }
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)

        viewModel.cargarDetalle("reserva-inexistente")
        runCurrent()

        assertFalse(viewModel.uiState.value.cargando)
        assertEquals("reserva_no_encontrada", viewModel.uiState.value.error)
    }

    @Test
    fun `cancelar reserva exitosa actualiza la entrada correspondiente en el listado`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()

        viewModel.cancelarReserva("motivo valido")
        runCurrent()

        assertEquals("CANCELADA", viewModel.uiState.value.reservas.single { it.id == RESERVA.id }.estado)
    }

    @Test
    fun `actuar con fallo expone el mensaje segun el codigo http`() = runTest {
        val repository = FakeReservaRepository().apply {
            rechazarResult = NetworkResult.Failure(403, "sin_permiso")
        }
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarSolicitud(SOLICITUD.id)
        runCurrent()

        viewModel.actuarSolicitud("rechazar", "responsable-1", "no cumple")
        runCurrent()

        assertEquals("No tienes permisos para realizar esta acción.", viewModel.uiState.value.error)
    }

    @Test
    fun `carga de solicitudes traduce cada codigo de error a un mensaje`() = runTest {
        val casos = mapOf(
            401 to "Tu sesión expiró.",
            403 to "No tienes permisos para realizar esta acción.",
            404 to "No se encontró el recurso.",
            409 to "Existe un conflicto de horario o estado.",
            500 to "No fue posible procesar la solicitud.",
        )
        casos.forEach { (codigo, mensaje) ->
            val repository = FakeReservaRepository().apply {
                listaSolicitudesResult = NetworkResult.Failure(codigo, "error")
            }
            val viewModel = ReservasViewModel(repository)
            runCurrent()
            assertEquals(mensaje, viewModel.uiState.value.error)
        }
    }

    @Test
    fun `carga de solicitudes sin conexion expone mensaje de sin conexion`() = runTest {
        val repository = FakeReservaRepository().apply {
            listaSolicitudesResult = NetworkResult.Failure(null, "sin_red")
        }
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        assertEquals("Sin conexión.", viewModel.uiState.value.error)
    }

    @Test
    fun `carga de solicitudes con no_implementado no sobrescribe el error`() = runTest {
        val repository = FakeReservaRepository().apply {
            listaSolicitudesResult = NetworkResult.Failure(null, "no_implementado")
        }
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `carga de reservas con fallo expone el mensaje traducido`() = runTest {
        val repository = FakeReservaRepository().apply {
            listadoResult = NetworkResult.Failure(500, "error")
        }
        val viewModel = ReservasViewModel(repository)
        runCurrent()
        assertEquals("No fue posible procesar la solicitud.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `fallo al cancelar reserva expone el mensaje del servidor`() = runTest {
        val repository = FakeReservaRepository().apply {
            cancelacionResult = NetworkResult.Failure(409, "conflicto_horario")
        }
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()

        viewModel.cancelarReserva("motivo valido")
        runCurrent()

        assertEquals("conflicto_horario", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.cancelando)
    }

    @Test
    fun `cancelar reserva con motivo en blanco no envia la solicitud`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()

        viewModel.cancelarReserva("   ")
        runCurrent()

        assertEquals(0, repository.cancelaciones)
    }

    @Test
    fun `consumir cancelacion exitosa limpia la bandera`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = ReservasViewModel(repository, cargarInicialmente = false)
        viewModel.cargarDetalle(RESERVA.id)
        runCurrent()
        viewModel.cancelarReserva("motivo valido")
        runCurrent()

        viewModel.consumirCancelacionExitosa()

        assertFalse(viewModel.uiState.value.cancelacionExitosa)
    }

    @Test
    fun `reserva rapida disponible crea solicitud una sola vez con idempotency key`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = NuevaReservaViewModel(repository)
        completarFormulario(viewModel)
        viewModel.enviar()
        viewModel.enviar()
        runCurrent()
        assertEquals(1, repository.creaciones)
        assertNotNull(repository.idempotencyKey)
        assertEquals(SOLICITUD.id, viewModel.uiState.value.solicitudCreada?.id)
        assertFalse(viewModel.uiState.value.enviando)
    }

    @Test
    fun `error al crear solicitud termina envio y conserva error`() = runTest {
        val repository = FakeReservaRepository().apply {
            creacionResult = NetworkResult.Failure(400, "gateway_http_400")
        }
        val viewModel = NuevaReservaViewModel(repository)
        completarFormulario(viewModel)
        viewModel.enviar()
        runCurrent()
        assertEquals("No fue posible procesar la solicitud.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.enviando)
    }

    @Test
    fun `enviar con campos incompletos marca error sin llamar al repositorio`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = NuevaReservaViewModel(repository)

        viewModel.enviar()
        runCurrent()

        assertEquals("Completa correctamente todos los campos obligatorios", viewModel.uiState.value.error)
        assertEquals(0, repository.creaciones)
    }

    @Test
    fun `comprobar disponibilidad con campos en blanco no consulta al repositorio`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = NuevaReservaViewModel(repository)

        viewModel.comprobarDisponibilidad()
        runCurrent()

        assertEquals(null, viewModel.uiState.value.disponible)
    }

    @Test
    fun `comprobar disponibilidad exitosa actualiza el estado`() = runTest {
        val repository = FakeReservaRepository()
        val viewModel = NuevaReservaViewModel(repository)
        viewModel.actualizarFormulario {
            it.copy(laboratorioId = "laboratorio-1", fechaReserva = "2026-08-20", horaInicio = "08:00", horaFin = "10:00")
        }

        viewModel.comprobarDisponibilidad()
        runCurrent()

        assertEquals(true, viewModel.uiState.value.disponible)
        assertFalse(viewModel.uiState.value.comprobando)
    }

    @Test
    fun `comprobar disponibilidad con fallo expone el mensaje segun el codigo`() = runTest {
        val repository = object : ReservaRepository by FakeReservaRepository() {
            override suspend fun consultarDisponibilidad(
                laboratorioId: String,
                fecha: String,
                horaInicio: String,
                horaFin: String,
            ) = NetworkResult.Failure(404, "no_encontrado")
        }
        val viewModel = NuevaReservaViewModel(repository)
        viewModel.actualizarFormulario {
            it.copy(laboratorioId = "laboratorio-1", fechaReserva = "2026-08-20", horaInicio = "08:00", horaFin = "10:00")
        }

        viewModel.comprobarDisponibilidad()
        runCurrent()

        assertEquals("No se encontró el recurso.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.comprobando)
    }

    private fun completarFormulario(viewModel: NuevaReservaViewModel) {
        viewModel.actualizarFormulario { it.copy(solicitanteId = "solicitante-1") }
        viewModel.actualizarFormulario { it.copy(docenteId = "docente-1") }
        viewModel.actualizarFormulario { it.copy(laboratorioId = "laboratorio-1") }
        viewModel.actualizarFormulario { it.copy(materiaId = "materia-1") }
        viewModel.actualizarFormulario { it.copy(periodoLectivoId = "periodo-1") }
        viewModel.actualizarFormulario { it.copy(fechaReserva = "2026-08-20") }
        viewModel.actualizarFormulario { it.copy(horaInicio = "08:00") }
        viewModel.actualizarFormulario { it.copy(horaFin = "10:00") }
        viewModel.actualizarFormulario { it.copy(numeroParticipantes = "20") }
        viewModel.actualizarFormulario { it.copy(motivo = "Clase") }
    }

    private class FakeReservaRepository : ReservaRepository {
        var listadoResult: NetworkResult<Pagina<Reserva>> = NetworkResult.Success(
            Pagina(listOf(RESERVA), 0, 20, 1, 1, primera = true, ultima = true),
        )
        var listaSolicitudesResult: NetworkResult<Pagina<SolicitudReserva>> = NetworkResult.Success(
            Pagina(emptyList(), 0, 20, 0, 0, primera = true, ultima = true),
        )
        var creacionResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD)
        var rechazarResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD.copy(estado = "RECHAZADA"))
        var aprobarResult: NetworkResult<Reserva> = NetworkResult.Success(RESERVA)
        var cancelacionResult: NetworkResult<Reserva> = NetworkResult.Success(RESERVA.copy(estado = "CANCELADA"))
        var obtenerSolicitudResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD)
        var obtenerResult: NetworkResult<Reserva> = NetworkResult.Success(RESERVA)
        var ponerEnRevisionResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD.copy(estado = "EN_REVISION"))
        var cancelarSolicitudResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD.copy(estado = "CANCELADA"))
        var responderPropuestaResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD.copy(estado = "PROGRAMADA"))
        var proponerResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD.copy(estado = "PROPUESTA"))
        var listados = 0
        var detalleId: String? = null
        var detallesSolicitud = 0
        var motivoCancelacion: String? = null
        var cancelaciones = 0
        var creaciones = 0
        var idempotencyKey: String? = null
        var aprobaciones = 0
        var rechazos = 0
        var comentarioRechazo: String? = null
        var revisiones = 0
        var cancelacionesSolicitud = 0
        var propuestasAceptadas = 0
        var propuestasRechazadas = 0
        var propuestas = 0

        override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> {
            listados++
            return listadoResult
        }
        override suspend fun obtener(id: String): NetworkResult<Reserva> {
            detalleId = id
            return obtenerResult
        }
        override suspend fun listarSolicitudes(): NetworkResult<Pagina<SolicitudReserva>> = listaSolicitudesResult
        override suspend fun obtenerSolicitud(id: String): NetworkResult<SolicitudReserva> {
            detallesSolicitud++
            return obtenerSolicitudResult
        }
        override suspend fun historial(id: String) =
            NetworkResult.Success(Pagina<HistorialSolicitud>(emptyList(), 0, 50, 0, 0, primera = true, ultima = true))
        override suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, idempotencyKey: String): NetworkResult<SolicitudReserva> {
            creaciones++
            this.idempotencyKey = idempotencyKey
            return creacionResult
        }
        override suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun cancelarSolicitud(id: String, comentario: String): NetworkResult<SolicitudReserva> {
            cancelacionesSolicitud++
            return cancelarSolicitudResult
        }
        override suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva> {
            cancelaciones++
            motivoCancelacion = motivo
            return cancelacionResult
        }
        override suspend fun ponerEnRevision(id: String): NetworkResult<SolicitudReserva> {
            revisiones++
            return ponerEnRevisionResult
        }
        override suspend fun aprobar(id: String, responsableId: String, comentario: String?, key: String): NetworkResult<Reserva> {
            aprobaciones++
            return aprobarResult
        }
        override suspend fun rechazar(id: String, comentario: String): NetworkResult<SolicitudReserva> {
            rechazos++
            comentarioRechazo = comentario
            return rechazarResult
        }
        override suspend fun proponer(id: String, propuesta: PropuestaAlternativa): NetworkResult<SolicitudReserva> {
            propuestas++
            return proponerResult
        }
        override suspend fun responderPropuesta(id: String, aceptar: Boolean, comentario: String?): NetworkResult<SolicitudReserva> {
            if (aceptar) propuestasAceptadas++ else propuestasRechazadas++
            return responderPropuestaResult
        }
        override suspend fun consultarDisponibilidad(
            laboratorioId: String,
            fecha: String,
            horaInicio: String,
            horaFin: String,
        ) = NetworkResult.Success(Disponibilidad(laboratorioId, fecha, horaInicio, horaFin, true, null))
    }

    private companion object {
        val RESERVA = Reserva(
            "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1",
            "2026-08-20", "08:00:00", "10:00:00", "PROGRAMADA", "RES-001",
            "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
        )
        val SOLICITUD = SolicitudReserva(
            "solicitud-1", "solicitante-1", "docente-1", "laboratorio-1", "materia-1",
            "periodo-1", "2026-08-20", "08:00", "10:00", 20, "Clase", null,
            "PENDIENTE", null, "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
        )
    }
}
