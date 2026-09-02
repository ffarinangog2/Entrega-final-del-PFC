package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.DocenciaData
import ec.edu.uteq.scli.mobile.features.institutional.data.HorarioDocenteDto
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.LaboratorioPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.MateriaPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PeriodoPlanificacionDto
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
class DocenteHorarioViewModelTest {
    private lateinit var repository: InstitutionalRepository
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); repository = mockk() }
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `carga horario propio con nombres humanos y sin editar planificacion`() = runTest {
        val data = DocenciaData(
            horarios = listOf(HorarioDocenteDto("h", "m", "p", "l", "d", "LUNES", "07:30", "09:30", true)),
            materias = listOf(MateriaPlanificacionDto("m", "c", "MAT", "Programación")),
            laboratorios = listOf(LaboratorioPlanificacionDto("l", "LAB-02", "Software", "DISPONIBLE")),
            periodo = PeriodoPlanificacionDto("p", "2026-B", "2026-B", "ACTIVO"),
        )
        coEvery { repository.docencia("perfil-autenticado") } returns data
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarDocencia("perfil-autenticado")

        coVerify(exactly = 1) { repository.docencia("perfil-autenticado") }
        assertEquals(data, viewModel.uiState.value.docencia)
    }
}
