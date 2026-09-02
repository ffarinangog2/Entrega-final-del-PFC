package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.CarreraPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.CoordinacionData
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.PeriodoPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionAgregadaDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPisoPlanificacionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: InstitutionalRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `aprueba todos los bloques enviados como una accion de pantalla`() = runTest {
        val enviados = listOf(plan("p-1"), plan("p-2"))
        coEvery { repository.coordinacion() } returns data(enviados)
        coEvery { repository.aprobarPlanificacionPiso("aggregate-1") } returns
            aggregate(enviados, "APROBADA")
        val viewModel = InstitutionalViewModel(repository)
        viewModel.cargarCoordinacion()

        viewModel.aprobarPaquete()

        coVerify(exactly = 1) { repository.aprobarPlanificacionPiso("aggregate-1") }
        assertEquals("Planificación aprobada", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `rechazo requiere motivo y propone observacion sobre bloque`() = runTest {
        val enviados = listOf(plan("p-1"))
        coEvery { repository.coordinacion() } returns data(enviados)
        coEvery {
            repository.proponerCambioPlanificacionPiso(any(), any(), any())
        } returns aggregate(enviados, "REQUIERE_CAMBIOS")
        val viewModel = InstitutionalViewModel(repository)
        viewModel.cargarCoordinacion()

        viewModel.rechazarPaquete("")
        assertEquals("No fue posible completar la operación", viewModel.uiState.value.error)
        viewModel.proponerCambio("p-1", "LAB en mantenimiento")

        coVerify {
            repository.proponerCambioPlanificacionPiso(
                "aggregate-1",
                "p-1",
                "LAB en mantenimiento",
            )
        }
        assertEquals("Observación enviada", viewModel.uiState.value.mensaje)
    }

    private fun data(planes: List<PlanificacionDto>) = CoordinacionData(
        planes, emptyList(), emptyList(), emptyList(),
        listOf(CarreraPlanificacionDto("carrera", "IS", "Ingeniería de Software")),
        PeriodoPlanificacionDto("periodo", "2026-B", "Periodo 2026-B", "ACTIVO"),
        planificacion = aggregate(planes),
    )

    private fun aggregate(
        planes: List<PlanificacionDto>,
        estado: String = "EN_REVISION",
    ) = PlanificacionAgregadaDto(
        "aggregate-1", "carrera", "periodo", estado, planes, emptyList(),
    )

    private fun plan(id: String, estado: String = "ENVIADA") = PlanificacionDto(
        id, "periodo", "carrera", "materia", "docente", "laboratorio",
        "LUNES", "07:30", "09:30", estado, null, "aggregate-1",
    )
}
