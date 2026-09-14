package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.RegistroAsistenciaDto
import ec.edu.uteq.scli.mobile.features.institutional.data.SesionAsistenciaDto
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
class EstudianteLaboratorioViewModelTest {
    private lateinit var repository: InstitutionalRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = mockk()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `carga sesiones de carrera e historial propio`() = runTest {
        coEvery { repository.sesionesAbiertas() } returns listOf(SESION)
        coEvery { repository.historial() } returns listOf(REGISTRO)
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarEstudiante()

        assertEquals(listOf(SESION), viewModel.uiState.value.sesionesAbiertas)
        assertEquals(listOf(REGISTRO), viewModel.uiState.value.historial)
    }

    @Test
    fun `registra identidad propia una vez y evita duplicado local`() = runTest {
        coEvery { repository.registrarPresenciaPropia("sesion-1") } returns REGISTRO
        coEvery { repository.sesionesAbiertas() } returns emptyList()
        coEvery { repository.historial() } returns listOf(REGISTRO)
        val viewModel = InstitutionalViewModel(repository)

        viewModel.registrarPresencia("sesion-1")
        viewModel.registrarPresencia("sesion-1")

        coVerify(exactly = 1) { repository.registrarPresenciaPropia("sesion-1") }
        assertEquals("Tu presencia fue registrada correctamente", viewModel.uiState.value.mensaje)
    }

    private companion object {
        val SESION = SesionAsistenciaDto("sesion-1", "reserva-1", "2026-09-01T10:00:00Z", "2026-09-01T10:15:00Z", "ABIERTA", null)
        val REGISTRO = RegistroAsistenciaDto("registro-1", "sesion-1", "estudiante-interno", "2026-09-01T10:05:00Z", "PRESENTE")
    }
}
