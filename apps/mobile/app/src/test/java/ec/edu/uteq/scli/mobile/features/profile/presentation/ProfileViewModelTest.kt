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

    private fun mockSettingsRepository(
        nombreUsuario: String = "",
        notificacionesHabilitadas: Boolean = true,
        temaOscuro: Boolean? = null,
        idiomaApp: String? = null,
    ): SettingsRepository {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.nombreUsuario } returns flowOf(nombreUsuario)
        every { settingsRepository.notificacionesHabilitadas } returns flowOf(notificacionesHabilitadas)
        every { settingsRepository.temaOscuro } returns flowOf(temaOscuro)
        every { settingsRepository.idiomaApp } returns flowOf(idiomaApp)
        return settingsRepository
    }

    @Test
    fun `uiState refleja los valores iniciales de nombreTecnico, notificacionesHabilitadas, temaOscuro e idiomaApp`() =
        runTest {
            val settingsRepository = mockSettingsRepository(
                nombreUsuario = "Juan Pérez",
                notificacionesHabilitadas = false,
                temaOscuro = true,
                idiomaApp = "en",
            )
            val viewModel = ProfileViewModel(settingsRepository)

            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val actual = viewModel.uiState.value
            assertEquals("Juan Pérez", actual.nombreUsuario)
            assertEquals(false, actual.notificacionesHabilitadas)
            assertEquals(true, actual.temaOscuro)
            assertEquals("en", actual.idiomaApp)
        }

    @Test
    fun `onNombreChange llama a settingsRepository setNombreTecnico con el valor correcto`() = runTest {
        val settingsRepository = mockSettingsRepository()
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onNombreChange("Ana Gómez")
        runCurrent()

        coVerify { settingsRepository.setNombreUsuario("Ana Gómez") }
    }

    @Test
    fun `onToggleNotificaciones llama a settingsRepository setNotificacionesHabilitadas con el valor correcto`() =
        runTest {
            val settingsRepository = mockSettingsRepository()
            val viewModel = ProfileViewModel(settingsRepository)

            viewModel.onToggleNotificaciones(false)
            runCurrent()

            coVerify { settingsRepository.setNotificacionesHabilitadas(false) }
        }

    @Test
    fun `onTemaChange llama a settingsRepository setTemaOscuro al forzar oscuro`() = runTest {
        val settingsRepository = mockSettingsRepository()
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onTemaChange(true)
        runCurrent()

        coVerify { settingsRepository.setTemaOscuro(true) }
    }

    @Test
    fun `onTemaChange llama a settingsRepository setTemaOscuro al volver al sistema`() = runTest {
        val settingsRepository = mockSettingsRepository()
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onTemaChange(null)
        runCurrent()

        coVerify { settingsRepository.setTemaOscuro(null) }
    }

    @Test
    fun `onIdiomaChange llama a settingsRepository setIdiomaApp al forzar ingles`() = runTest {
        val settingsRepository = mockSettingsRepository()
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onIdiomaChange("en")
        runCurrent()

        coVerify { settingsRepository.setIdiomaApp("en") }
    }

    @Test
    fun `onIdiomaChange llama a settingsRepository setIdiomaApp al volver al sistema`() = runTest {
        val settingsRepository = mockSettingsRepository()
        val viewModel = ProfileViewModel(settingsRepository)

        viewModel.onIdiomaChange(null)
        runCurrent()

        coVerify { settingsRepository.setIdiomaApp(null) }
    }
}
