package ec.edu.uteq.scli.mobile.features.reservas.presentation

import ec.edu.uteq.scli.mobile.common.network.NetworkResult
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosRepository
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.CatalogosSolicitud
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.DocenteDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.EstadoPeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.HorarioDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.LaboratorioCatalogoDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.MateriaDto
import ec.edu.uteq.scli.mobile.features.reservas.data.remote.PeriodoDto
import ec.edu.uteq.scli.mobile.features.reservas.domain.ActualizacionSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Disponibilidad
import ec.edu.uteq.scli.mobile.features.reservas.domain.NuevaSolicitudReserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.Pagina
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import ec.edu.uteq.scli.mobile.features.reservas.domain.ReservaRepository
import ec.edu.uteq.scli.mobile.features.reservas.domain.SolicitudReserva
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaReservaCatalogosTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository: ReservaRepository = object : ReservaRepository {
        override suspend fun listar(pagina: Int, tamanio: Int) = NetworkResult.Failure(null, "no_usado")
        override suspend fun obtener(id: String) = NetworkResult.Failure(null, "no_usado")
        override suspend fun crearSolicitud(solicitud: NuevaSolicitudReserva, idempotencyKey: String) =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun actualizarSolicitud(id: String, solicitud: ActualizacionSolicitudReserva) =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun cancelarSolicitud(id: String, comentario: String) =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun cancelarReserva(id: String, motivo: String) =
            NetworkResult.Failure(null, "no_usado")
        override suspend fun consultarDisponibilidad(
            laboratorioId: String,
            fecha: String,
            horaInicio: String,
            horaFin: String,
        ) = NetworkResult.Failure(null, "no_usado")
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `cargar catalogos exitoso filtra materias activas segun horarios del docente y laboratorios activos`() = runTest {
        val catalogos = mockk<CatalogosRepository>()
        coEvery { catalogos.cargar("perfil-1") } returns CatalogosSolicitud(
            docente = DocenteDto("docente-1", "perfil-1", "DOC-1", true),
            materias = listOf(
                MateriaDto("m1", "MAT-1", "Redes", activo = true),
                MateriaDto("m2", "MAT-2", "Bases de datos", activo = true),
                MateriaDto("m3", "MAT-3", "Inactiva", activo = false),
            ),
            periodo = PeriodoDto("p1", "2026-A", "2026 A", EstadoPeriodoDto.ACTIVO),
            laboratorios = listOf(
                LaboratorioCatalogoDto("l1", "LAB-1", "Redes", "piso-1", true),
                LaboratorioCatalogoDto("l2", "LAB-2", "Inactivo", "piso-1", false),
            ),
            horarios = listOf(HorarioDto("h1", "docente-1", "m1", "p1", "l1")),
        )

        val viewModel = NuevaReservaViewModel(repository, catalogos, perfilId = "perfil-1")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(false, state.cargandoCatalogos)
        assertEquals("docente-1", state.docenteId)
        assertEquals("DOC-1", state.docenteCodigo)
        assertEquals(listOf("m1"), state.materias.map { it.id })
        assertEquals(listOf("l1"), state.laboratorios.map { it.id })
        assertEquals("p1", state.periodo?.id)
    }

    @Test
    fun `cargar catalogos con fallo expone un error legible`() = runTest {
        val catalogos = mockk<CatalogosRepository>()
        coEvery { catalogos.cargar(any()) } throws IllegalStateException("servicio caido")

        val viewModel = NuevaReservaViewModel(repository, catalogos, perfilId = "perfil-1")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(false, state.cargandoCatalogos)
        assertEquals("No fue posible cargar los catálogos", state.error)
    }

    @Test
    fun `sin catalogos no queda cargando`() = runTest {
        val viewModel = NuevaReservaViewModel(repository)
        runCurrent()

        assertTrue(!viewModel.uiState.value.cargandoCatalogos)
    }
}
