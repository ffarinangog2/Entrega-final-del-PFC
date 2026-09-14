package ec.edu.uteq.scli.mobile.features.reservas.presentation

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ec.edu.uteq.scli.mobile.features.reservas.domain.Reserva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class ReservasContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows reservation and opens selected detail`() {
        var selectedId: String? = null
        composeRule.setContent {
            ReservasContent(
                state = ReservasUiState(reservas = listOf(RESERVA)),
                onRefresh = {}, onRetry = {},
                onReservaClick = { selectedId = it }, onNuevaReserva = {},
            )
        }
        composeRule.onNodeWithText("Reservas").performClick()
        composeRule.onNodeWithTag("reserva_${RESERVA.id}").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(RESERVA.id, selectedId) }
    }

    @Test
    fun `shows loading state`() {
        composeRule.setContent {
            ReservasContent(
                state = ReservasUiState(cargando = true),
                onRefresh = {}, onRetry = {}, onReservaClick = {}, onNuevaReserva = {},
            )
        }
        composeRule.onNodeWithTag("reservas_loading").assertExists()
    }

    @Test
    fun `shows error and invokes retry action`() {
        var retried = false
        composeRule.setContent {
            ReservasContent(
                state = ReservasUiState(error = "gateway_no_disponible"),
                onRefresh = {}, onRetry = { retried = true }, onReservaClick = {}, onNuevaReserva = {},
            )
        }
        composeRule.onNodeWithText("Reintentar").performClick()
        composeRule.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun `shows empty requests state`() {
        composeRule.setContent {
            ReservasContent(
                state = ReservasUiState(),
                onRefresh = {}, onRetry = {}, onReservaClick = {}, onNuevaReserva = {},
            )
        }
        composeRule.onNodeWithText("No hay solicitudes").assertExists()
    }

    private companion object {
        val RESERVA = Reserva(
            "reserva-1", "solicitud-1", "laboratorio-1", "responsable-1",
            "2026-08-20", "08:00:00", "10:00:00", "PROGRAMADA", "RES-001",
            "2026-08-18T10:00:00Z", "2026-08-18T10:00:00Z", 0,
        )
    }
}
