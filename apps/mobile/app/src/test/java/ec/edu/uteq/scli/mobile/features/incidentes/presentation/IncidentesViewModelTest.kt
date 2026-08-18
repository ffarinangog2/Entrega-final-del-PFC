package ec.edu.uteq.scli.mobile.features.incidentes.presentation

import ec.edu.uteq.scli.mobile.features.incidentes.domain.Incidente
import ec.edu.uteq.scli.mobile.features.incidentes.domain.IncidenteRepository
import ec.edu.uteq.scli.mobile.features.notifications.NotificationHelper
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class IncidentesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeIncidenteRepository : IncidenteRepository {
        private val state = MutableStateFlow<List<Incidente>>(emptyList())
        private var nextId = 1L

        override fun observarTodos() = state.asStateFlow()

        override suspend fun crear(incidente: Incidente): Incidente {
            val creado = incidente.copy(id = nextId++)
            state.value = state.value + creado
            return creado
        }
    }

    @Test
    fun `guardar incidente con campos completos lo agrega al listado y notifica`() = runTest {
        val repository = FakeIncidenteRepository()
        val notificationHelper = mockk<NotificationHelper>(relaxed = true)
        val viewModel = IncidentesViewModel(repository, notificationHelper)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onLaboratorioEquipoChange("Lab 3 - PC 12")
        viewModel.onDescripcionChange("No enciende")
        viewModel.onGuardarIncidente()
        runCurrent()

        val actual = viewModel.uiState.value
        assertEquals(1, actual.incidentes.size)
        assertEquals("Lab 3 - PC 12", actual.incidentes.first().laboratorioEquipo)
        assertEquals("", actual.laboratorioEquipo)
        assertEquals("", actual.descripcion)
        verify { notificationHelper.mostrar(any(), any()) }
    }

    @Test
    fun `guardar incidente sin campos obligatorios no lo agrega y marca error`() = runTest {
        val repository = FakeIncidenteRepository()
        val notificationHelper = mockk<NotificationHelper>(relaxed = true)
        val viewModel = IncidentesViewModel(repository, notificationHelper)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        viewModel.onGuardarIncidente()
        runCurrent()

        val actual = viewModel.uiState.value
        assertTrue(actual.incidentes.isEmpty())
        assertEquals("campos_incompletos", actual.error)
        verify(exactly = 0) { notificationHelper.mostrar(any(), any()) }
    }
}
