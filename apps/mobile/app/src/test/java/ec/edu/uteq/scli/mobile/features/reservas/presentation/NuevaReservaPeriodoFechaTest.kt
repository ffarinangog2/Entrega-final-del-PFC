package ec.edu.uteq.scli.mobile.features.reservas.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.EstadoPeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
class NuevaReservaPeriodoFechaTest {

    private val dispatcher = StandardTestDispatcher()
    private val fechaHoy = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `A - inicioPeriodo mayor a hoy define fechaMinima igual a fechaInicio`() {
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-B",
            nombre = "Periodo 2026-B",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-08-01",
            fechaFin = "2026-12-31"
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.Valido)
        val valido = rango as PeriodoReservaRango.Valido
        assertEquals(LocalDate.of(2026, 8, 1), valido.fechaMinima)
        assertEquals(LocalDate.of(2026, 12, 31), valido.fechaMaxima)
    }

    @Test
    fun `B - inicioPeriodo menor o igual a hoy define fechaMinima igual a hoy`() {
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo 2026-A",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-03-01",
            fechaFin = "2026-07-31"
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.Valido)
        val valido = rango as PeriodoReservaRango.Valido
        assertEquals(fechaHoy, valido.fechaMinima)
        assertEquals(LocalDate.of(2026, 7, 31), valido.fechaMaxima)
    }

    @Test
    fun `C - fechaMaxima coincide con fechaFin`() {
        val fin = LocalDate.of(2026, 10, 30)
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-01-01",
            fechaFin = fin.toString()
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.Valido)
        val valido = rango as PeriodoReservaRango.Valido
        assertEquals(fin, valido.fechaMaxima)
    }

    @Test
    fun `D - dia anterior al minimo no es seleccionable en SelectableDates`() {
        val min = LocalDate.of(2026, 6, 15)
        val max = LocalDate.of(2026, 7, 31)
        val selectable = ReservaSelectableDates(min, max)

        val diaAnterior = min.minusDays(1)
        val utcMillis = localDateToUtcMillis(diaAnterior)
        assertFalse(selectable.isSelectableDate(utcMillis))
    }

    @Test
    fun `E - dia posterior al maximo no es seleccionable en SelectableDates`() {
        val min = LocalDate.of(2026, 6, 15)
        val max = LocalDate.of(2026, 7, 31)
        val selectable = ReservaSelectableDates(min, max)

        val diaPosterior = max.plusDays(1)
        val utcMillis = localDateToUtcMillis(diaPosterior)
        assertFalse(selectable.isSelectableDate(utcMillis))
    }

    @Test
    fun `F - dia exactamente fechaMinima es seleccionable en SelectableDates`() {
        val min = LocalDate.of(2026, 6, 15)
        val max = LocalDate.of(2026, 7, 31)
        val selectable = ReservaSelectableDates(min, max)

        val utcMillis = localDateToUtcMillis(min)
        assertTrue(selectable.isSelectableDate(utcMillis))
    }

    @Test
    fun `G - dia exactamente fechaMaxima es seleccionable en SelectableDates`() {
        val min = LocalDate.of(2026, 6, 15)
        val max = LocalDate.of(2026, 7, 31)
        val selectable = ReservaSelectableDates(min, max)

        val utcMillis = localDateToUtcMillis(max)
        assertTrue(selectable.isSelectableDate(utcMillis))
    }

    @Test
    fun `H - sin periodo deshabilita envio y expone mensaje seguro`() = runTest {
        val repo = TestReservaRepository()
        val viewModel = NuevaReservaViewModel(
            repository = repo,
            hoyProvider = { fechaHoy }
        )
        viewModel.actualizarFormulario {
            it.copy(
                periodo = null,
                periodoLectivoId = "",
                solicitanteId = "s-1",
                docenteId = "d-1",
                laboratorioId = "l-1",
                materiaId = "m-1",
                fechaReserva = "2026-06-20",
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "15",
                motivo = "Clase"
            )
        }
        viewModel.enviar()
        runCurrent()

        assertEquals("No hay un período lectivo activo para realizar reservas.", viewModel.uiState.value.error)
        assertEquals(0, repo.creaciones)
    }

    @Test
    fun `I - fechaInicio null determina rango invalido`() {
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = null,
            fechaFin = "2026-12-31"
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.FechasNoDeterminadas)
        assertEquals("No se pudo determinar el rango de fechas del período lectivo.", (rango as PeriodoReservaRango.FechasNoDeterminadas).mensaje)
    }

    @Test
    fun `J - fechaFin null determina rango invalido`() {
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-01-01",
            fechaFin = null
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.FechasNoDeterminadas)
        assertEquals("No se pudo determinar el rango de fechas del período lectivo.", (rango as PeriodoReservaRango.FechasNoDeterminadas).mensaje)
    }

    @Test
    fun `K - fecha con formato corrupto no causa crash y determina rango invalido`() {
        val periodo = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "fecha-invalida",
            fechaFin = "2026-12-31"
        )
        val rango = evaluarRangoPeriodo(periodo, hoy = fechaHoy)
        assertTrue(rango is PeriodoReservaRango.FechasNoDeterminadas)
    }

    @Test
    fun `L - periodo vencido fechaFin anterior a hoy deshabilita envio con mensaje seguro`() = runTest {
        val periodoVencido = PeriodoDto(
            id = "p1",
            codigo = "2025-B",
            nombre = "Periodo Vencido",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2025-01-01",
            fechaFin = "2025-12-31"
        )
        val repo = TestReservaRepository()
        val viewModel = NuevaReservaViewModel(
            repository = repo,
            hoyProvider = { fechaHoy }
        )
        viewModel.actualizarFormulario {
            it.copy(
                periodo = periodoVencido,
                periodoLectivoId = "p1",
                solicitanteId = "s-1",
                docenteId = "d-1",
                laboratorioId = "l-1",
                materiaId = "m-1",
                fechaReserva = "2025-06-20",
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "15",
                motivo = "Clase"
            )
        }
        viewModel.enviar()
        runCurrent()

        assertEquals("El período lectivo activo ha finalizado o no permite nuevas reservas.", viewModel.uiState.value.error)
        assertEquals(0, repo.creaciones)
    }

    @Test
    fun `M - seleccionar nueva fecha limpia disponible error y solicitudCreada`() = runTest {
        val repo = TestReservaRepository()
        val viewModel = NuevaReservaViewModel(
            repository = repo,
            hoyProvider = { fechaHoy }
        )
        val solicitudDummy = SolicitudReserva(
            id = "s-1", solicitanteId = "u-1", docenteId = "d-1", laboratorioId = "l-1",
            materiaId = "m-1", periodoLectivoId = "p-1", fechaReserva = "2026-06-20",
            horaInicio = "08:00", horaFin = "10:00", numeroParticipantes = 10,
            motivo = "Test", observacion = null, estado = "PENDIENTE",
            reservaId = null, creadaEn = "2026-06-15T00:00:00Z",
            actualizadaEn = "2026-06-15T00:00:00Z", version = 0
        )
        viewModel.actualizar { it.copy(fechaReserva = "2026-06-20") }
        viewModel.actualizarFormulario {
            it.copy(
                disponible = true,
                error = "Un error previo",
                solicitudCreada = solicitudDummy
            )
        }
        assertEquals(true, viewModel.uiState.value.disponible)
        assertEquals("Un error previo", viewModel.uiState.value.error)
        assertEquals(solicitudDummy, viewModel.uiState.value.solicitudCreada)

        // Selección de nueva fecha
        viewModel.actualizar { it.copy(fechaReserva = "2026-06-25") }

        assertEquals(null, viewModel.uiState.value.disponible)
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.solicitudCreada)
        assertEquals("2026-06-25", viewModel.uiState.value.fechaReserva)
    }

    @Test
    fun `N - conversion DatePicker UTC a LocalDate no cambia el dia en zona UTC-5`() {
        val fechaDeseada = LocalDate.of(2026, 8, 20)
        val utcMillis = localDateToUtcMillis(fechaDeseada)

        // Nuestra función con ZoneOffset.UTC
        val fechaConvertida = utcMillisToLocalDate(utcMillis)
        assertEquals(fechaDeseada, fechaConvertida)

        // Verificamos que si se usara erróneamente la zona de Ecuador (UTC-5), cambiaría al día anterior
        val fechaConZonaEcuador = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("America/Guayaquil")).toLocalDate()
        assertEquals(fechaDeseada.minusDays(1), fechaConZonaEcuador)
    }

    @Test
    fun `O - ViewModel rechaza una fecha fuera de rango sin llamar repository`() = runTest {
        val periodoValido = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo 2026-A",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-06-01",
            fechaFin = "2026-06-30"
        )
        val repo = TestReservaRepository()
        val viewModel = NuevaReservaViewModel(
            repository = repo,
            hoyProvider = { fechaHoy } // hoy = 2026-06-15 -> rango: 2026-06-15 .. 2026-06-30
        )
        viewModel.actualizarFormulario {
            it.copy(
                periodo = periodoValido,
                periodoLectivoId = "p1",
                solicitanteId = "s-1",
                docenteId = "d-1",
                laboratorioId = "l-1",
                materiaId = "m-1",
                fechaReserva = "2026-06-10", // Fuera de rango (anterior a hoy)
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "15",
                motivo = "Clase"
            )
        }
        viewModel.enviar()
        runCurrent()

        assertEquals(0, repo.creaciones)
        assertEquals(
            "La fecha de la reserva debe estar comprendida entre 2026-06-15 y 2026-06-30 para el período académico seleccionado.",
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `P - HTTP 400 backend mantiene mensaje seguro de P0-A`() = runTest {
        val periodoValido = PeriodoDto(
            id = "p1",
            codigo = "2026-A",
            nombre = "Periodo 2026-A",
            estado = EstadoPeriodoDto.ACTIVO,
            fechaInicio = "2026-06-01",
            fechaFin = "2026-06-30"
        )
        val mensajeBackend400 = "El laboratorio se encuentra en mantenimiento en la fecha seleccionada."
        val repo = TestReservaRepository().apply {
            creacionResult = NetworkResult.Failure(400, mensajeBackend400)
        }
        val viewModel = NuevaReservaViewModel(
            repository = repo,
            hoyProvider = { fechaHoy }
        )
        viewModel.actualizarFormulario {
            it.copy(
                periodo = periodoValido,
                periodoLectivoId = "p1",
                solicitanteId = "s-1",
                docenteId = "d-1",
                laboratorioId = "l-1",
                materiaId = "m-1",
                fechaReserva = "2026-06-20",
                horaInicio = "08:00",
                horaFin = "10:00",
                numeroParticipantes = "15",
                motivo = "Clase"
            )
        }
        viewModel.enviar()
        runCurrent()

        assertEquals(1, repo.creaciones)
        assertEquals(mensajeBackend400, viewModel.uiState.value.error)
    }

    private class TestReservaRepository : ReservaRepository {
        var creaciones = 0
        var creacionResult: NetworkResult<SolicitudReserva> = NetworkResult.Success(
            SolicitudReserva(
                id = "s-1", solicitanteId = "s-1", docenteId = "d-1", laboratorioId = "l-1",
                materiaId = "m-1", periodoLectivoId = "p1", fechaReserva = "2026-06-20",
                horaInicio = "08:00", horaFin = "10:00", numeroParticipantes = 15,
                motivo = "Clase", observacion = null, estado = "PENDIENTE",
                reservaId = null, creadaEn = "2026-06-15T00:00:00Z",
                actualizadaEn = "2026-06-15T00:00:00Z", version = 0
            )
        )

        override suspend fun listar(pagina: Int, tamanio: Int): NetworkResult<Pagina<Reserva>> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun obtener(id: String): NetworkResult<Reserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun listarSolicitudes(): NetworkResult<Pagina<SolicitudReserva>> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun obtenerSolicitud(id: String): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun historial(id: String): NetworkResult<Pagina<ec.edu.uteq.scli.mobile.features.reservas.domain.HistorialSolicitud>> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, idempotencyKey: String): NetworkResult<SolicitudReserva> {
            creaciones++
            return creacionResult
        }
        override suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun cancelarSolicitud(id: String, comentario: String): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun cancelarReserva(id: String, motivo: String): NetworkResult<Reserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun ponerEnRevision(id: String): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun aprobar(id: String, responsableId: String, comentario: String?, key: String): NetworkResult<Reserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun rechazar(id: String, comentario: String): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun proponer(id: String, propuesta: ec.edu.uteq.scli.mobile.features.reservas.domain.PropuestaAlternativa): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun responderPropuesta(id: String, aceptar: Boolean, comentario: String?): NetworkResult<SolicitudReserva> =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun consultarDisponibilidad(
            laboratorioId: String,
            fecha: String,
            horaInicio: String,
            horaFin: String
        ): NetworkResult<ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad> =
            NetworkResult.Failure(null, "no_usado")
    }
}
