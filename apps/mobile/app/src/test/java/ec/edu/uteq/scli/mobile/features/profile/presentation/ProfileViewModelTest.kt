package ec.edu.uteq.scli.mobile.features.profile.presentation

import ec.edu.uteq.scli.mobile.features.profile.data.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState refleja los valores iniciales de nombreTecnico y notificacionesHabilitadas`() = runTest {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.nombreTecnico } returns flowOf("Juan Pérez")
        every { settingsRepository.notificacionesHabilitadas } returns flowOf(false)
        val viewModel = ProfileViewModel(settingsRepository)

        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()

        val actual = viewModel.uiState.value
        assertEquals("Juan Pérez", actual.nombreTecnico)
        assertEquals(false, actual.notificacionesHabilitadas)
    }

    @Test
    fun `onNombreChange llama a settingsRepository setNombreTecnico con el valor correcto`() = runTest {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.nombreTecnico } returns flowOf("")
        every { settingsRepository.notificacionesHabilitadas } returns flowOf(true)
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onNombreChange("Ana Gómez")
        runCurrent()

        coVerify { settingsRepository.setNombreTecnico("Ana Gómez") }
    }

    @Test
    fun `onToggleNotificaciones llama a settingsRepository setNotificacionesHabilitadas con el valor correcto`() = runTest {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.nombreTecnico } returns flowOf("")
        every { settingsRepository.notificacionesHabilitadas } returns flowOf(true)
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onToggleNotificaciones(false)
        runCurrent()

        coVerify { settingsRepository.setNotificacionesHabilitadas(false) }
    }
}
