package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
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
        assertEquals("gateway_http_503", viewModel.uiState.value.error)
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
        assertEquals("gateway_http_400", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.enviando)
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
        var creacionResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(SOLICITUD)
        var listados = 0
        var detalleId: String? = null
        var motivoCancelacion: String? = null
        var creaciones = 0
        var idempotencyKey: String? = null

        override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> {
            listados++
            return listadoResult
        }
        override suspend fun obtener(id: String): NetworkResult<Reserva> {
            detalleId = id
            return NetworkResult.Success(RESERVA)
        }
        override suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, idempotencyKey: String): NetworkResult<SolicitudReserva> {
            creaciones++
            this.idempotencyKey = idempotencyKey
            return creacionResult
        }
        override suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun cancelarSolicitud(id: String, comentario: String) =
            NetworkResult.Failure(501, "no_usado")
        override suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva> {
            motivoCancelacion = motivo
            return NetworkResult.Success(RESERVA.copy(estado = "CANCELADA"))
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
