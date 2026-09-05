package ec.edu.uteq.scli.mobile.features.institutional.presentation

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ec.edu.uteq.scli.mobile.features.institutional.data.CarreraPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.CoordinacionData
import ec.edu.uteq.scli.mobile.features.institutional.data.DocentePlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.LaboratorioPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.MateriaPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PeriodoPlanificacionDto
import ec.edu.uteq.scli.mobile.features.institutional.data.PlanificacionDto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w600dp-h1600dp")
class CoordinacionContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `muestra carrera periodo asignacion y nombres humanos sin uuid`() {
        composeRule.setContent { CoordinacionContent(estado()) }

        composeRule.onNodeWithText("Carrera: Ingeniería de Software").assertIsDisplayed()
        composeRule.onNodeWithText("Periodo: 2026-B").assertIsDisplayed()
        composeRule.onNodeWithText("Programación Web").assertIsDisplayed()
        composeRule.onNodeWithText("Docente: DOC-CARLOS").assertIsDisplayed()
        composeRule.onNodeWithText("LAB-01").assertIsDisplayed()
        composeRule.onNodeWithText("materia-uuid").assertDoesNotExist()
    }

    @Test
    fun `agrupa por dia y permite cambiar a martes`() {
        composeRule.setContent { CoordinacionContent(estado(conPlanMartes = true)) }

        composeRule.onNodeWithText("Bases de Datos").assertDoesNotExist()
        composeRule.onNodeWithText("Martes").performClick()
        composeRule.onNodeWithText("Bases de Datos").assertIsDisplayed()
    }

    @Test
    fun `muestra plan aprobado en consulta y observaciones controladas`() {
        composeRule.setContent {
            CoordinacionContent(estado(estado = "CONFIRMADA", observacion = "Revisada por administración"))
        }

        composeRule.onNodeWithText("Estado de planificación: APROBADA").assertIsDisplayed()
        composeRule.onNodeWithText("Observación: Revisada por administración").assertIsDisplayed()
        composeRule.onNodeWithText("Editar").assertDoesNotExist()
    }

    @Test
    fun `muestra error controlado sin datos`() {
        composeRule.setContent {
            CoordinacionContent(InstitutionalUiState(error = "No fue posible completar la operación"))
        }

        composeRule.onNodeWithText("No fue posible completar la operación").assertIsDisplayed()
    }

    @Test
    fun `consulta disponibilidad humana de laboratorios`() {
        composeRule.setContent { CoordinacionContent(estado()) }

        composeRule.onNodeWithText("Disponibilidad de laboratorios").performClick()
        composeRule.onNodeWithText("Disponible").assertIsDisplayed()
        composeRule.onNodeWithText("Laboratorio de Software").assertIsDisplayed()
    }

    private fun estado(
        estado: String = "BORRADOR",
        observacion: String? = null,
        conPlanMartes: Boolean = false,
    ): InstitutionalUiState {
        val planes = mutableListOf(plan("plan-1", "LUNES", "materia-uuid", estado, observacion))
        if (conPlanMartes) planes += plan("plan-2", "MARTES", "materia-2", estado, null)
        return InstitutionalUiState(
            coordinacion = CoordinacionData(
                planificaciones = planes,
                materias = listOf(
                    MateriaPlanificacionDto("materia-uuid", "carrera-uuid", "PROG", "Programación Web"),
                    MateriaPlanificacionDto("materia-2", "carrera-uuid", "BDD", "Bases de Datos"),
                ),
                docentes = listOf(DocentePlanificacionDto("docente-uuid", "DOC-CARLOS")),
                laboratorios = listOf(
                    LaboratorioPlanificacionDto("laboratorio-uuid", "LAB-01", "Laboratorio de Software", "DISPONIBLE"),
                ),
                carreras = listOf(CarreraPlanificacionDto("carrera-uuid", "IS", "Ingeniería de Software")),
                periodo = PeriodoPlanificacionDto("periodo-uuid", "2026-B", "Periodo 2026-B", "ACTIVO"),
            ),
        )
    }

    private fun plan(id: String, dia: String, materiaId: String, estado: String, observacion: String?) =
        PlanificacionDto(
            id, "periodo-uuid", "carrera-uuid", materiaId, "docente-uuid", "laboratorio-uuid",
            dia, "07:30", "09:30", estado, observacion,
        )
}
