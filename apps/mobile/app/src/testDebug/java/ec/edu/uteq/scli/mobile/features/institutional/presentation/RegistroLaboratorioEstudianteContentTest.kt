package ec.edu.uteq.scli.mobile.features.institutional.presentation

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ec.edu.uteq.scli.mobile.features.institutional.data.RegistroAsistenciaDto
import ec.edu.uteq.scli.mobile.features.institutional.data.SesionAsistenciaDto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w600dp-h1200dp")
class RegistroLaboratorioEstudianteContentTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `muestra registro humano sin ids ni token y registra sesion internamente`() {
        var registrada: String? = null
        composeRule.setContent {
            RegistroLaboratorioEstudianteContent(
                state = InstitutionalUiState(sesionesAbiertas = listOf(SESION)),
                onRegistrar = { registrada = it }, onActualizar = {},
            )
        }

        composeRule.onNodeWithText("Registro habilitado").assertIsDisplayed()
        composeRule.onNodeWithText("Registrar mi presencia").performClick()
        composeRule.onNodeWithText("sesion-uuid").assertDoesNotExist()
        composeRule.onNodeWithText("token-secreto").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals("sesion-uuid", registrada) }
    }

    @Test
    fun `muestra doble registro y estados vacios`() {
        composeRule.setContent {
            RegistroLaboratorioEstudianteContent(
                state = InstitutionalUiState(sesionesAbiertas = listOf(SESION), historial = listOf(REGISTRO)),
                onRegistrar = {}, onActualizar = {},
            )
        }
        composeRule.onNodeWithText("Tu presencia ya fue registrada en esta sesión.").assertIsDisplayed()
        composeRule.onNodeWithText("Registrar mi presencia").assertDoesNotExist()
    }

    private companion object {
        val SESION = SesionAsistenciaDto("sesion-uuid", "reserva-uuid", "2026-09-01T10:00:00Z", "2026-09-01T10:15:00Z", "ABIERTA", "token-secreto")
        val REGISTRO = RegistroAsistenciaDto("registro", "sesion-uuid", "estudiante", "2026-09-01T10:05:00Z", "PRESENTE")
    }
}
