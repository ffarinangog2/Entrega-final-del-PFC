package ec.edu.uteq.scli.mobile.features.institutional.presentation

import ec.edu.uteq.scli.mobile.features.institutional.data.EstudianteHorarioData
import ec.edu.uteq.scli.mobile.features.institutional.data.InstitutionalRepository
import ec.edu.uteq.scli.mobile.features.institutional.data.LaboratorioPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.MateriaPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PeriodoPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionBloqueDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EstudianteHorarioViewModelTest {
    private lateinit var repository: InstitutionalRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = mockk()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `carga horario de estudiante con materias laboratorios y periodo`() = runTest {
        val bloque = PlanificacionBloqueDto(
            id = "bloque-1",
            materiaId = "mat-1",
            periodoId = "per-1",
            laboratorioId = "lab-1",
            diaSemana = "LUNES",
            horaInicio = "07:30",
            horaFin = "09:30",
            estado = "CONFIRMADA",
            nivel = 3,
        )
        val data = EstudianteHorarioData(
            horarios = listOf(bloque),
            materias = listOf(MateriaPlanificacionDto("mat-1", "carr-1", "PROG", "Programación Web", 3)),
            laboratorios = listOf(LaboratorioPlanificacionDto("lab-1", "LAB-01", "Laboratorio de Software", "DISPONIBLE")),
            periodo = PeriodoPlanificacionDto("per-1", "2026-B", "Periodo 2026-B", "ACTIVO"),
        )
        coEvery { repository.estudianteHorario() } returns data
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarHorarioEstudiante()

        coVerify(exactly = 1) { repository.estudianteHorario() }
        assertEquals(data, viewModel.uiState.value.estudianteHorario)
        assertNull(viewModel.uiState.value.error)

        val cargado = requireNotNull(viewModel.uiState.value.estudianteHorario)
        val materiasMap = cargado.materias.associateBy { it.id }
        val labsMap = cargado.laboratorios.associateBy { it.id }
        val item = cargado.horarios.single()

        assertEquals("Programación Web", materiasMap[item.materiaId]?.nombre)
        assertEquals("LAB-01 — Laboratorio de Software", labsMap[item.laboratorioId]?.let { "${it.codigo} — ${it.nombre}" })
        assertEquals("LUNES", item.diaSemana)
        assertEquals("07:30", item.horaInicio)
        assertEquals("09:30", item.horaFin)
        assertEquals(3, item.nivel)
    }

    @Test
    fun `estudianteHorario sin periodo actual mantiene horario vacio y no lanza error a la UI fail closed`() = runTest {
        val data = EstudianteHorarioData(
            horarios = emptyList(),
            materias = listOf(MateriaPlanificacionDto("mat-1", "carr-1", "PROG", "Programación Web")),
            laboratorios = listOf(LaboratorioPlanificacionDto("lab-1", "LAB-01", "Software", "DISPONIBLE")),
            periodo = null,
        )
        coEvery { repository.estudianteHorario() } returns data
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarHorarioEstudiante()

        coVerify(exactly = 1) { repository.estudianteHorario() }
        assertEquals(data, viewModel.uiState.value.estudianteHorario)
        assertTrue(viewModel.uiState.value.estudianteHorario?.horarios?.isEmpty() == true)
        assertNull(viewModel.uiState.value.estudianteHorario?.periodo)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `bloque con laboratorio null resuelve aula por confirmar`() = runTest {
        val bloque = PlanificacionBloqueDto(
            id = "bloque-sin-lab",
            materiaId = "mat-1",
            periodoId = "per-1",
            laboratorioId = null,
            diaSemana = "MARTES",
            horaInicio = "10:00",
            horaFin = "12:00",
            estado = "CONFIRMADA",
        )
        val data = EstudianteHorarioData(
            horarios = listOf(bloque),
            materias = listOf(MateriaPlanificacionDto("mat-1", "carr-1", "RED", "Redes")),
            laboratorios = emptyList(),
            periodo = PeriodoPlanificacionDto("per-1", "2026-B", "Periodo 2026-B", "ACTIVO"),
        )
        coEvery { repository.estudianteHorario() } returns data
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarHorarioEstudiante()

        val cargado = requireNotNull(viewModel.uiState.value.estudianteHorario)
        val item = cargado.horarios.single()
        val labsMap = cargado.laboratorios.associateBy { it.id }
        val etiquetaLab = item.laboratorioId?.let { labsMap[it]?.let { lab -> "${lab.codigo} — ${lab.nombre}" } } ?: "Aula por confirmar"

        assertEquals("Aula por confirmar", etiquetaLab)
    }

    @Test
    fun `error de red se propaga a la ui amigablemente`() = runTest {
        coEvery { repository.estudianteHorario() } throws RuntimeException("Error de conexión con el gateway")
        val viewModel = InstitutionalViewModel(repository)

        viewModel.cargarHorarioEstudiante()

        assertEquals("No fue posible completar la operación", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.estudianteHorario)
    }
}
