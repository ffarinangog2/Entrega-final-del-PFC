package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.AdministracionData
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.LaboratorioPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PerfilAdminDto
import io.mockk.coEvery
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
class AdministradorGlobalViewModelTest {
    private lateinit var repository: InstitutionalRepository

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); repository = mockk() }
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `carga perfiles laboratorios y planificacion con alcance global`() = runTest {
        val data = AdministracionData(
            perfiles = listOf(PerfilAdminDto("interno", "Ana", "Admin", "ana@uteq.edu.ec", true)),
            laboratorios = listOf(LaboratorioPlanificacionDto("interno-lab", "LAB-1", "Software", "DISPONIBLE")),
            planificaciones = emptyList(),
        )
        coEvery { repository.administracion() } returns data
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarAdministracion()

        assertEquals(data, viewModel.uiState.value.administracion)
    }
}
